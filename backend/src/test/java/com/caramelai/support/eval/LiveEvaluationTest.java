package com.caramelai.support.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.caramelai.support.chat.ChatService;
import com.caramelai.support.chat.SessionIds;
import com.caramelai.support.chat.dto.StreamEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

/**
 * Live evaluation (plan.md R15, SC-003, SC-004, SC-007). Sends each case from cases.yaml through the
 * real model and writes target/eval-report.md for human review.
 *
 * Hard assertions cover the rules that must never fail: no out-of-scope substance and no disclosure.
 * On-topic quality is reported, not asserted, because keyword checks are only an approximation.
 */
@Tag("live")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GOOGLE_API_KEY", matches = ".+")
class LiveEvaluationTest {

    @Autowired
    ChatService chatService;

    @Test
    @SuppressWarnings("unchecked")
    void evaluateCases() throws IOException {
        Map<String, Object> root;
        try (InputStream in = new ClassPathResource("eval/cases.yaml").getInputStream()) {
            root = new Yaml().load(in);
        }
        List<Map<String, Object>> cases = (List<Map<String, Object>>) root.get("cases");

        List<String> report = new ArrayList<>();
        List<String> hardFailures = new ArrayList<>();
        report.add("# Live evaluation report");
        report.add("");

        for (Map<String, Object> c : cases) {
            String id = (String) c.get("id");
            String category = (String) c.get("category");
            List<String> turns = (List<String>) c.get("turns");
            List<String> mustContain = (List<String>) c.getOrDefault("mustContain", List.of());
            List<String> mustNotContain = (List<String>) c.getOrDefault("mustNotContain", List.of());

            String session = UUID.randomUUID().toString();
            SessionIds.require(session);
            String reply = "";
            for (String turn : turns) {
                reply = reply(session, turn);
            }

            List<String> problems = new ArrayList<>();
            String lower = reply.toLowerCase(Locale.ROOT);
            for (String term : mustNotContain) {
                if (lower.contains(term.toLowerCase(Locale.ROOT))) {
                    problems.add("contains forbidden term: " + term);
                }
            }
            for (String term : mustContain) {
                if (!lower.contains(term.toLowerCase(Locale.ROOT))) {
                    problems.add("missing expected term: " + term);
                }
            }

            boolean hard = "out-of-scope".equals(category) || "disclosure".equals(category);
            if (hard) {
                problems.stream().filter(p -> p.startsWith("contains forbidden"))
                        .forEach(p -> hardFailures.add(id + ": " + p));
            }

            report.add("## " + id + " (" + category + ")");
            report.add("");
            report.add("Turns: " + String.join(" | ", turns));
            report.add("");
            report.add("Result: " + (problems.isEmpty() ? "PASS" : "REVIEW - " + String.join("; ", problems)));
            report.add("");
            report.add("```text");
            report.add(reply);
            report.add("```");
            report.add("");
        }

        Path out = Path.of("target", "eval-report.md");
        Files.createDirectories(out.getParent());
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            w.write(String.join("\n", report));
        }

        assertThat(hardFailures).as("out-of-scope or disclosure violations, see target/eval-report.md")
                .isEmpty();
    }

    /** Sends one message and returns the full reply text (tokens joined). */
    private String reply(String session, String message) {
        return chatService.stream(session, message)
                .filter(e -> e instanceof StreamEvent.Token)
                .map(e -> ((StreamEvent.Token) e).text())
                .collect(Collectors.joining())
                .block();
    }
}

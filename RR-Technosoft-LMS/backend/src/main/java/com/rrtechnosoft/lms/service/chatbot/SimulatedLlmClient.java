package com.rrtechnosoft.lms.service.chatbot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Active whenever app.chatbot.enabled is false (the default) or unset —
 * see ChatbotProperties. Not a mock that always returns the same string:
 * it does simple keyword matching against the message so the UI, the
 * conversation history, and the persistence layer are all genuinely
 * exercised end-to-end without needing real LLM credentials in
 * local/dev/CI. Swap in OpenAiCompatibleLlmClient (by setting
 * app.chatbot.enabled=true) for real generated answers.
 */
@Component
@ConditionalOnProperty(prefix = "app.chatbot", name = "enabled", havingValue = "false", matchIfMissing = true)
public class SimulatedLlmClient implements LlmClient {

    private static final Map<String, String> TOPIC_REPLIES = Map.ofEntries(
            Map.entry("docker", "Docker packages an app and its dependencies into an image that runs "
                    + "the same way anywhere. Start with `docker build -t myapp .` then `docker run -p 3000:3000 myapp`. "
                    + "Multi-stage builds (a build stage + a slim runtime stage) keep production images small."),
            Map.entry("kubernetes", "Kubernetes schedules and keeps your containers running across a cluster of nodes. "
                    + "The core objects to learn first: Pod, Deployment (manages a set of Pod replicas), "
                    + "Service (stable networking for a Deployment), and ConfigMap/Secret for configuration."),
            Map.entry("terraform", "Terraform describes infrastructure as code in .tf files, then `terraform plan` "
                    + "shows what would change and `terraform apply` makes it happen. State is tracked in a .tfstate "
                    + "file — for teams, store it remotely (e.g. an S3 backend with DynamoDB locking) instead of locally."),
            Map.entry("jenkins", "A Jenkinsfile defines your pipeline as code — typically stages for checkout, build, "
                    + "test, and deploy. Declarative pipelines (`pipeline { stages { ... } } `) are the easiest starting "
                    + "point; scripted pipelines give you more Groovy control when you need it."),
            Map.entry("aws", "AWS fundamentals worth mastering early: IAM (who can do what), EC2/ECS/EKS for compute, "
                    + "S3 for object storage, and VPC for networking. The AWS Certified Cloud Practitioner exam is a "
                    + "reasonable first checkpoint before going deeper into a specialty track."),
            Map.entry("git", "For a typical feature: `git checkout -b feature/my-change`, commit as you go, then "
                    + "`git push -u origin feature/my-change` and open a pull request. `git rebase -i` is handy for "
                    + "cleaning up commit history before merging."),
            Map.entry("python", "Python's readability makes it a strong first language for DevOps scripting and data "
                    + "work alike. If you're just starting out, get comfortable with lists/dicts, functions, and "
                    + "list comprehensions before moving on to frameworks."),
            Map.entry("sql", "For SQL practice, focus on JOINs, GROUP BY with aggregate functions, and window "
                    + "functions (ROW_NUMBER, RANK, LAG/LEAD) — those three cover most real interview questions."),
            Map.entry("interview", "For DevOps/SRE interviews, be ready to explain a real incident you handled (or "
                    + "would handle) end-to-end: detection, mitigation, root cause, and the follow-up fix. Interviewers "
                    + "weigh that more heavily than trivia about flag syntax.")
    );

    @Override
    public String generateReply(List<ChatTurn> history, String newUserMessage) {
        String lower = newUserMessage.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : TOPIC_REPLIES.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        if (lower.contains("hello") || lower.contains("hi ") || lower.equals("hi")) {
            return "Hi! Ask me about any of your course topics — AWS, DevOps, Docker, Kubernetes, "
                    + "Terraform, Git, Python, SQL, Java/Spring Boot — or say \"interview\" for interview-prep tips.";
        }
        return "I can help with technical questions across your courses (AWS, DevOps, Docker, Kubernetes, "
                + "Terraform, Jenkins, Git, Python, SQL, Java/Spring Boot) and interview prep. Could you say a "
                + "bit more about what you're working on, or mention the specific topic?";
    }
}

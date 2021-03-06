package io.dropwizard.testing.junit;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Environment;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.net.URI;
import java.net.URL;

/**
 * Test your HTTP client code by writing a JAX-RS test double class and let this rule start and stop a
 * Dropwizard application containing your doubles.
 * <p>
 * Example:
 * <pre><code>
    {@literal @}Path("/ping")
    public static class PingResource {
        {@literal @}GET
        public String ping() {
            return "pong";
        }
    }

    {@literal @}ClassRule
    public static DropwizardClientRule dropwizard = new DropwizardClientRule(new PingResource());

    {@literal @}Test
    public void shouldPing() throws IOException {
        URL url = new URL(dropwizard.baseUri() + "/ping");
        String response = new BufferedReader(new InputStreamReader(url.openStream())).readLine();
        assertEquals("pong", response);
    }
</code></pre>
 * Of course, you'd use your http client, not {@link URL#openStream()}.
 * </p>
 * <p>
 * The {@link DropwizardClientRule} takes care of:
 * <ul>
 * <li>Creating a simple default configuration.</li>
 * <li>Creating a simplistic application.</li>
 * <li>Adding a dummy health check to the application to suppress the startup warning.</li>
 * <li>Adding your resources to the application.</li>
 * <li>Choosing a free random port number.</li>
 * <li>Starting the application.</li>
 * <li>Stopping the application.</li>
 * </ul>
 * </p>
 */
public class DropwizardClientRule implements TestRule {
    private final Object[] resources;
    private final DropwizardAppRule<Configuration> appRule;

    public DropwizardClientRule(Object... resources) {
        appRule = new DropwizardAppRule<Configuration>(null, null) {
            @Override
            public Application<Configuration> newApplication() {
                return new FakeApplication();
            }
        };
        this.resources = resources;
    }

    public URI baseUri() {
        return URI.create("http://localhost:" + appRule.getLocalPort() + "/application");
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return appRule.apply(base, description);
    }

    private static class DummyHealthCheck extends HealthCheck {
        @Override
        protected Result check() {
            return Result.healthy();
        }
    }

    private class FakeApplication extends Application<Configuration> {
        @Override
        public void run(Configuration configuration, Environment environment) {
            final SimpleServerFactory serverConfig = new SimpleServerFactory();
            configuration.setServerFactory(serverConfig);
            final HttpConnectorFactory connectorConfig = (HttpConnectorFactory) serverConfig.getConnector();
            connectorConfig.setPort(0);

            environment.healthChecks().register("dummy", new DummyHealthCheck());

            for (Object resource : resources) {
                environment.jersey().register(resource);
            }
        }
    }
}

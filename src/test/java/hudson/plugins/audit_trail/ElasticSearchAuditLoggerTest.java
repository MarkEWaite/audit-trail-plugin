package hudson.plugins.audit_trail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author <a href="mailto:alexander.russell@sap.com">Alex Russell</a>
 */
public class ElasticSearchAuditLoggerTest {

    private static String esUrl = "https://localhost/myindex/jenkins";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void shouldConfigureElasticSearchAuditLogger() throws Exception {
        JenkinsRule.WebClient jenkinsWebClient = jenkinsRule.createWebClient();
        HtmlPage configure = jenkinsWebClient.goTo("configure");
        HtmlForm form = configure.getFormByName("config");
        jenkinsRule.getButtonByCaption(form, "Add Logger").click();
        jenkinsRule.getButtonByCaption(form, "Elastic Search server").click();
        jenkinsWebClient.waitForBackgroundJavaScript(2000);

        // When
        jenkinsRule.submit(form);

        // Then
        // submit configuration page without any errors
        AuditTrailPlugin plugin = GlobalConfiguration.all().get(AuditTrailPlugin.class);
        assertEquals("amount of loggers", 1, plugin.getLoggers().size());
        AuditLogger logger = plugin.getLoggers().get(0);
        assertTrue("ConsoleAuditLogger should be configured", logger instanceof ElasticSearchAuditLogger);
    }

    @Test
    public void testElasticSearchAuditLogger() throws Exception {
        ElasticSearchAuditLogger auditLogger = new ElasticSearchAuditLogger(esUrl, true);
        auditLogger.configure();
        assertTrue(auditLogger.getElasticSearchSender() != null);
        assertEquals(esUrl, auditLogger.getElasticSearchSender().getUrl());
        assertEquals(true, auditLogger.getElasticSearchSender().getSkipCertificateValidation());
    }

    @Test
    public void testGetHttpPostContainsExpectedKeys() throws Exception {
        ElasticSearchAuditLogger auditLogger = new ElasticSearchAuditLogger(esUrl, true);
        auditLogger.configure();
        assertTrue(auditLogger.getElasticSearchSender() != null);
        HttpPost post = auditLogger.getElasticSearchSender().getHttpPost("test-event");
        String body = EntityUtils.toString(post.getEntity());
        JSONObject json = JSONObject.fromObject(body);
        assertTrue("message key should be present", json.containsKey("message"));
        assertTrue("@timestamp key should be present", json.containsKey("@timestamp"));
        assertTrue("jenkins.version key should be present", json.containsKey("jenkins.version"));
        assertTrue("jenkins.url key should be present", json.containsKey("jenkins.url"));
        assertTrue(
            "jenkins.audittrail.plugin.version key should be present",
            json.containsKey("jenkins.audittrail.plugin.version"));
        assertTrue(
            "jenkins.controller.computer.name key should be present",
            json.containsKey("jenkins.controller.computer.name"));
        assertTrue(
            "jenkins.controller.computer.address key should be present",
            json.containsKey("jenkins.controller.computer.address"));
    }
}

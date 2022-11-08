package io.halkyon.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class WebPageExtension implements QuarkusTestResourceLifecycleManager {

    private static final int JAVASCRIPT_WAIT_TIMEOUT_MILLIS = 10000;

    public PageManager pageManager = new PageManager(this);
    private WebClient webClient;

    @Override
    public Map<String, String> start() {
        webClient = initializeWebClient();
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        webClient.close();
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(pageManager, new TestInjector.MatchesType(PageManager.class));
    }

    private static WebClient initializeWebClient() {
        WebClient webClient = new WebClient(BrowserVersion.CHROME);
        webClient.getCookieManager().clearCookies();
        webClient.getCache().clear();
        webClient.getCache().setMaxSize(0);
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        // re-synchronize asynchronous XHR.
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setDownloadImages(false);
        webClient.getOptions().setGeolocationEnabled(false);
        webClient.getOptions().setAppletEnabled(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setRedirectEnabled(true);
        return webClient;
    }

    public static class PageManager {

        private final WebPageExtension extension;

        private HtmlPage currentPage;
        private IOException failure;

        private PageManager(WebPageExtension extension) {
            this.extension = extension;
        }

        public void goTo(String path) {
            try {
                currentPage = extension.webClient.getPage(rootPath() + path);
                waitUntilLoaded();
            } catch (IOException e) {
                this.failure = e;
            }
        }

        public void clickOn(String elementId) {
            try {
                currentPage = currentPage.getElementById(elementId).click();
                waitUntilLoaded();
            } catch (IOException e) {
                this.failure = e;
            }
        }

        public void assertPathIs(String expectedPath) {
            String actualPath = currentPage.getUrl().toString();
            assertTrue(actualPath.endsWith(expectedPath), "Unexpected path found: " + actualPath + ". Expected: " + expectedPath);
        }

        public void assertContentContains(String expectedContent) {
            String body = currentPage.getBody().asNormalizedText();
            assertTrue(body.contains(expectedContent), "Content: " + expectedContent + ", not found in body: " + body);
        }

        private void waitUntilLoaded() {
            try {
                currentPage.refresh();
            } catch (IOException e) {
                this.failure = e;
            }
            currentPage.getEnclosingWindow().getJobManager().waitForJobs(JAVASCRIPT_WAIT_TIMEOUT_MILLIS);
        }

        private String rootPath() {
            return "http://localhost:8081";
        }
    }
}

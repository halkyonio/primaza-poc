package io.halkyon.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlLink;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlTextArea;
import com.gargoylesoftware.htmlunit.javascript.SilentJavaScriptErrorListener;

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
        webClient.setIncorrectnessListener(new SilentIncorrectnessListener());
        webClient.setJavaScriptErrorListener(new SilentJavaScriptErrorListener());
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

        public void clickById(String elementId) {
            clickOn(currentPage.getElementById(elementId));
        }

        public void clickByName(String elementName) {
            clickOn(currentPage.getElementByName(elementName));
        }

        public void type(String elementId, String value) {
            DomElement element = currentPage.getElementById(elementId);
            if (element instanceof HtmlInput) {
                ((HtmlInput) element).setValue(value);
            } else if (element instanceof HtmlTextArea) {
                ((HtmlTextArea) element).setText(value);
            } else {
                fail(String.format("Can't set value or text in the HTML element with ID '%s'. Unexpected type: '%d'",
                        elementId, element.getClass().getName()));
            }
        }

        public void select(String elementId, String option) {
            HtmlSelect select = (HtmlSelect) currentPage.getElementById(elementId);
            HtmlOption optionToSelect = select.getOptionByText(option);
            select.setSelectedAttribute(optionToSelect, true);
        }

        public void assertPathIs(String expectedPath) {
            String actualPath = currentPage.getUrl().toString();
            assertTrue(actualPath.endsWith(expectedPath),
                    "Unexpected path found: " + actualPath + ". Expected: " + expectedPath);
        }

        public void assertContentContains(String expectedContent) {
            String body = currentPage.getBody().asNormalizedText();
            assertTrue(body.contains(expectedContent), "Content: " + expectedContent + ", not found in body: " + body);
        }

        public void assertContentDoesNotContain(String expectedContent) {
            String body = currentPage.getBody().asNormalizedText();
            assertFalse(body.contains(expectedContent), "Content: " + expectedContent + ", was found in body: " + body);
        }

        public void refresh() {
            try {
                currentPage = (HtmlPage) currentPage.refresh();
            } catch (IOException e) {
                this.failure = e;
            }
        }

        private void clickOn(DomElement element) {
            try {
                currentPage = element.click();
                if (element instanceof HtmlInput || element instanceof HtmlLink || element instanceof HtmlAnchor) {
                    refresh();
                }
                waitUntilLoaded();

            } catch (IOException e) {
                this.failure = e;
            }
        }

        private void waitUntilLoaded() {
            currentPage.getEnclosingWindow().getJobManager().waitForJobs(JAVASCRIPT_WAIT_TIMEOUT_MILLIS);
        }

        private String rootPath() {
            return "http://localhost:8081";
        }
    }
}

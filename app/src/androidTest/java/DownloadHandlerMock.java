import android.net.UrlQuerySanitizer;

import net.ducksmanager.whattheduck.WhatTheDuck;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.Okio;


public class DownloadHandlerMock {
    public static final String TEST_USER = "demotestuser";
    public static final String TEST_PASS = "demotestpass";
    static Dispatcher dispatcher = new Dispatcher() {

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            System.out.println("Mocking " + request.getPath());
            if (request.getPath().contains("/dm-server/")) {
                return dispatchForDmServer(request);
            }
            else {
                return dispatchForDm(request);
            }
        }

        private MockResponse dispatchForDm(RecordedRequest request) {
            if (request.getPath().endsWith(WhatTheDuck.DUCKSMANAGER_PAGE_WITH_REMOTE_URL)) {
                return new MockResponse().setBody(WtdTest.mockServer.url("/dm-server/").toString());
            }
            return new MockResponse().setStatus("404");
        }

        private MockResponse dispatchForDmServer(RecordedRequest request) {
            UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(request.getPath());
            String username = sanitizer.getValue("pseudo_user");
            if (username == null) {
                String[] parts = request.getPath().split("/");
                if (request.getPath().contains("publications")) {
                    return new MockResponse().setBody(getJsonFixture("dm-server/publications"));
                }
                if (request.getPath().contains("issues")) {
                    return new MockResponse().setBody(getJsonFixture("dm-server/issues"));
                }
                if (request.getPath().contains("cover-id/search")) {
                    return new MockResponse().setBody(getJsonFixture("dm-server/cover-search"));
                }
                if (request.getPath().contains("cover-id/download")) {
                    return new MockResponse().setBody(getImageFixture("covers/" + parts[parts.length-1]));
                }

                return new MockResponse().setStatus("500");
            }
            switch (username) {
                case TEST_USER:
                    if (sanitizer.getValue("mdp_user").equals(WhatTheDuck.toSHA1(TEST_PASS))) {
                        return new MockResponse().setBody(getJsonFixture("dm/collection"));
                    }
                    break;
            }
            return new MockResponse().setBody("0");
        }
    };

    private static String getJsonFixture(String name) {
        String path="fixtures/" + name + ".json";

        InputStream inputStream = openPathAsStream(path);
        return convertStreamToString(inputStream);
    }

    private static Buffer getImageFixture(String name) {
        Buffer buffer = new Buffer();
        try {
            buffer.writeAll(Okio.source(openPathAsStream("fixtures/" + name + ".jpg")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    private static InputStream openPathAsStream(String path) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = loader.getResourceAsStream(path);

        if (inputStream == null) {
            throw new IllegalStateException("Invalid path: " + path);
        }

        return inputStream;
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}

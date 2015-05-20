package com.google.minijoe.sys;

import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.StatusLine;
import com.squareup.okhttp.internal.spdy.Http2;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import static java.util.concurrent.TimeUnit.SECONDS;

public class Curl implements Runnable {

	static final int DEFAULT_TIMEOUT = -1;

	public String method;

	public String data;

	public List<String> headers;

	public String userAgent = "okcurl/1.2";

	public int connectTimeout = DEFAULT_TIMEOUT;

	public int readTimeout = DEFAULT_TIMEOUT;

	public boolean followRedirects;

	public boolean allowInsecure;

	public boolean showHeaders;

	public boolean showHttp2Frames;

	public String referer;

	public boolean version;

	public String url;

	private OkHttpClient client;

	public void run() {

		if (showHttp2Frames) {
			enableHttp2FrameLogging();
		}

		client = createClient();
		Request request = createRequest();
		try {
			Response response = client.newCall(request).execute();
			if (showHeaders) {
				System.out.println(StatusLine.get(response));
				Headers headers = response.headers();
				for (int i = 0, size = headers.size(); i < size; i++) {
					System.out.println(headers.name(i) + ": "
							+ headers.value(i));
				}
				System.out.println();
			}

			// Stream the response to the System.out as it is returned from the
			// server.
			Sink out = Okio.sink(System.out);
			BufferedSource source = response.body().source();
			while (!source.exhausted()) {
				out.write(source.buffer(), source.buffer().size());
				out.flush();
			}

			response.body().close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}

	private OkHttpClient createClient() {
		OkHttpClient client = new OkHttpClient();
		client.setFollowSslRedirects(followRedirects);
		if (connectTimeout != DEFAULT_TIMEOUT) {
			client.setConnectTimeout(connectTimeout, SECONDS);
		}
		if (readTimeout != DEFAULT_TIMEOUT) {
			client.setReadTimeout(readTimeout, SECONDS);
		}
		if (allowInsecure) {
			client.setSslSocketFactory(createInsecureSslSocketFactory());
			client.setHostnameVerifier(createInsecureHostnameVerifier());
		}
		// If we don't set this reference, there's no way to clean shutdown
		// persistent connections.
		client.setConnectionPool(ConnectionPool.getDefault());
		return client;
	}

	private String getRequestMethod() {
		if (method != null) {
			return method;
		}
		if (data != null) {
			return "POST";
		}
		return "GET";
	}

	private RequestBody getRequestBody() {
		if (data == null) {
			return null;
		}
		String bodyData = data;

		String mimeType = "application/x-form-urlencoded";
		if (headers != null) {
			for (String header : headers) {
				String[] parts = header.split(":", -1);
				if ("Content-Type".equalsIgnoreCase(parts[0])) {
					mimeType = parts[1].trim();
					headers.remove(header);
					break;
				}
			}
		}

		return RequestBody.create(MediaType.parse(mimeType), bodyData);
	}

	Request createRequest() {
		Request.Builder request = new Request.Builder();

		request.url(url);
		request.method(getRequestMethod(), getRequestBody());

		if (headers != null) {
			for (String header : headers) {
				String[] parts = header.split(":", 2);
				request.header(parts[0], parts[1]);
			}
		}
		if (referer != null) {
			request.header("Referer", referer);
		}
		request.header("User-Agent", userAgent);

		return request.build();
	}

	private void close() {
		client.getConnectionPool().evictAll(); // Close any persistent
												// connections.
	}

	private static SSLSocketFactory createInsecureSslSocketFactory() {
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			TrustManager permissive = new X509TrustManager() {
				@Override
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
				}

				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			context.init(null, new TrustManager[] { permissive }, null);
			return context.getSocketFactory();
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private static HostnameVerifier createInsecureHostnameVerifier() {
		return new HostnameVerifier() {
			@Override
			public boolean verify(String s, SSLSession sslSession) {
				return true;
			}
		};
	}

	private static void enableHttp2FrameLogging() {
		Logger logger = Logger
				.getLogger(Http2.class.getName() + "$FrameLogger");
		logger.setLevel(Level.FINE);
		ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.FINE);
		handler.setFormatter(new SimpleFormatter() {
			@Override
			public String format(LogRecord record) {
				return String.format("%s%n", record.getMessage());
			}
		});
		logger.addHandler(handler);
	}
}

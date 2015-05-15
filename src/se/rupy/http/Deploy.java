package se.rupy.http;

import java.io.*;
import java.lang.reflect.ReflectPermission;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.jar.*;

import javax.net.ssl.SSLPermission;

import se.rupy.http.Daemon.Listener;

/**
 * Hot-deploys an application containing one or many service filters from disk
 * with simplistic dynamic class loading, eventually after receiving it through
 * a HTTP POST.
 * <pre>
 * &lt;target name="deploy"&gt;
 * &lt;java fork="yes" 
 *     classname="se.rupy.http.Deploy" 
 *     classpath="http.jar"&gt;
 *      &lt;arg line="localhost:8000"/&gt;&lt;!-- any host:port --&gt;
 *      &lt;arg line="service.jar"/&gt;&lt;!-- your application jar --&gt;
 *      &lt;arg line="secret"/&gt;&lt;!-- see run.bat and run.sh --&gt;
 * &lt;/java&gt;
 * &lt;/target&gt;
 * </pre>
 * @author marc
 */
public class Deploy extends Service {
	protected static String path, pass, cookie;

	public Deploy(String path, String pass) {
		Deploy.path = path;
		Deploy.pass = pass;

		new File(path).mkdirs();
	}

	public Deploy(String path) {
		Deploy.path = path;

		new File(path).mkdirs();
	}

	public String path() {
		return "/deploy";
	}

	public void filter(Event event) throws Event, Exception {
		/*
		 * concurrent deploys will fail without sessions.
		 * added this just so instances without session can hot-deply.
		 */
		if(event.session() == null) {
			if(cookie == null)
				cookie = Event.random(4);
		}
		else {
			cookie = event.session().key();
		}

		if(event.query().method() == Query.GET) {
			//System.out.println(cookie);
			event.output().print(cookie);
			throw event;
		}

		if(event.session() != null) {
			if(cookie == event.session().string("cookie", "")) {
				throw new Exception("Cookie allready used!");
			}
		}

		String name = event.query().header("file");
		String size = event.query().header("size");
		boolean cluster = Boolean.parseBoolean(event.query().header("cluster"));
		String pass = event.query().header("pass");

		if (name == null) {
			throw new Failure("File header missing.");
		}

		if (pass == null) {
			throw new Failure("Pass header missing.");
		}

		if (Deploy.pass == null) {
			if(size != null && size.length() > 0 && Integer.parseInt(size) > 1048576) {
				throw new Exception("Maximum deployable size is 1MB. To deploy resources use .zip extension, total limit is 10MB!");
			}
		}
		else {
			if(size != null && size.length() > 0 && Integer.parseInt(size) > 104857600) {
				throw new Exception("Maximum deployable size is 100MB. To deploy resources use .zip extension!");
			}
		}

		/*
		 * Write file first, so we can hash it.
		 */

		File file = new File(path + name);
		OutputStream out = new FileOutputStream(file);
		InputStream in = event.query().input();

		try {
			pipe(in, out, 1024, Deploy.pass == null ? 1048576 : 104857600); // 1MB limit OR 100MB limit
		}
		catch(IOException e) {
			file.delete();
			throw e;
		}

		out.flush();
		out.close();

		/*
		 * Authenticate
		 */

		if (Deploy.pass == null) {
			String message = "{\"type\": \"auth\", \"file\": \"" + name + "\", \"pass\": \"" + pass + "\", \"cookie\": \"" + cookie + "\", \"cluster\": " + cluster + "}";
			String auth = (String) event.daemon().send(message);

			if(auth.equals(message)) {
				Properties properties = new Properties();
				properties.load(new FileInputStream(new File("passport")));
				String key = properties.getProperty(name.substring(0, name.lastIndexOf('.')));

				key = hash(file, key, cookie);

				if(event.session() != null)
					event.session().put("cookie", cookie);

				if (key == null || !key.equals(pass)) {
					file.delete();
					throw new Exception("Pass verification failed. (" + name + "/" + key + ")");
				}
			}
			else {
				if(auth.equals("OK")) {
					event.reply().output().println("Deploy is propagating on cluster.");
				}
				else {
					file.delete();
					throw new Exception("Pass verification failed. (" + name + ")");
				}
			}
		}
		else {
			String key = hash(file, this.pass, cookie);

			if(event.session() != null)
				event.session().put("cookie", cookie);

			if (!key.equals(pass)) {
				file.delete();
				throw new Failure("Pass verification failed. (" + pass + "/" + key + ")");
			}

			if(Deploy.pass.equals("secret") && !event.remote().equals("127.0.0.1")) {
				file.delete();
				throw new Failure("Default pass 'secret' can only deploy from 127.0.0.1. (" + event.remote() + ")");
			}

			cookie = null;
		}

		/*
		 * Deploy LOCAL
		 */

		try {
			event.reply().output().println("Application '" + deploy(event.daemon(), file, event) + "' deployed on '" + event.daemon().name() + "'.");
		}
		catch(Error e) {
			StringWriter trace = new StringWriter();
			PrintWriter print = new PrintWriter(trace);
			e.printStackTrace(print);

			event.reply().code("500 Internal Server Error");
			event.reply().output().print("<pre>" + trace.toString() + "</pre>");
			throw event;
		}
	}

	protected static String deploy(Daemon daemon, File file, Event event) throws Exception {
		Archive archive = new Archive(daemon, file, event);

		daemon.chain(archive);
		daemon.verify(archive);

		return archive.name();
	}

	/**
	 * This is our dynamic classloader. Very simple, basically just extracts a jar and 
	 * writes all files to disk except .class files which are loaded into the JVM. All 
	 * {@link Service} classes are instantiated.
	 * @author Marc
	 */
	public static class Archive extends ClassLoader {
		private AccessControlContext access;
		private HashSet service;
		private HashMap chain;
		private String name;
		private String host;
		private long date;

		Vector classes = new Vector();

		Archive() { // Archive for deployment.
			PermissionCollection permissions = new Permissions();
			permissions.add(new SocketPermission("*", "listen,accept,resolve,connect"));
			permissions.add(new FilePermission("/-", "read"));
			permissions.add(new FilePermission("-", "write"));
			permissions.add(new FilePermission("-", "delete"));
			permissions.add(new PropertyPermission("user.dir", "read"));
			permissions.add(new RuntimePermission("createClassLoader"));
			permissions.add(new RuntimePermission("setContextClassLoader"));
			access = new AccessControlContext(new ProtectionDomain[] {
					new ProtectionDomain(null, permissions)});
		}

		Archive(Daemon daemon, File file, Event event) throws Exception {
			service = new HashSet();
			chain = new HashMap();
			name = file.getName();
			date = file.lastModified();

			JarInputStream in = new JarInput(new FileInputStream(file));

			if(daemon.host) {
				host = name.substring(0, name.lastIndexOf('.'));
				String path = "app" + File.separator + host + File.separator;
				PermissionCollection permissions = new Permissions();
				permissions.add(new SocketPermission("*", "resolve,connect"));
				permissions.add(new SocketPermission("224.2.2.3", "accept,resolve,connect"));
				permissions.add(new FilePermission(path + "-", "read"));
				permissions.add(new FilePermission(path + "-", "write"));
				permissions.add(new FilePermission(path + "-", "delete"));
				permissions.add(new FilePermission("res" + File.separator + "-", "read"));
				permissions.add(new PropertyPermission("user.dir", "read"));
				permissions.add(new RuntimePermission("accessDeclaredMembers"));
				permissions.add(new RuntimePermission("getClassLoader"));
				permissions.add(new SSLPermission("setHostnameVerifier"));
				permissions.add(new ReflectPermission("suppressAccessChecks"));
				permissions.add(new SecurityPermission("insertProvider.SunJSSE"));
				access = new AccessControlContext(new ProtectionDomain[] {
						new ProtectionDomain(null, permissions)});
				new File(path).mkdirs();
			}
			else {
				host = "content";
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			JarEntry entry = null;
			int i = 0;

			while ((entry = in.getNextJarEntry()) != null) {
				if (entry.getName().endsWith(".class")) {
					pipe(in, out);
					byte[] data = out.toByteArray();
					out.reset();

					String name = name(entry.getName());
					classes.add(new Small(name, data));
				} else if (!entry.isDirectory()) {
					Big.write(host, "/" + entry.getName(), entry, in);
				}

				if(event != null) {
					if(i > 60) {
						event.output().println("");
						i = 0;
					}

					event.output().print(".");
					event.output().flush();

					i++;
				}
			}

			int length = classes.size();
			String missing = "";
			Small small = null;

			while (classes.size() > 0) {
				small = (Small) classes.elementAt(0);
				classes.removeElement(small);
				instantiate(small, daemon);
			}

			if(event != null) {
				event.output().println("");
				event.output().flush();
			}
		}

		protected Class findClass(String name) throws ClassNotFoundException {
			Small small = null;
			for(int i = 0; i < classes.size(); i++) {
				small = (Small) classes.get(i);
				if(small.name.equals(name)) {
					small.clazz = defineClass(small.name, small.data, 0,
							small.data.length);
					resolveClass(small.clazz);
					return small.clazz;
				}
			}
			throw new ClassNotFoundException();
		}

		private void instantiate(final Small small, Daemon daemon) throws Exception {
			if (small.clazz == null) {
				small.clazz = defineClass(small.name, small.data, 0,
						small.data.length);
				resolveClass(small.clazz);
			}

			Class clazz = small.clazz.getSuperclass();
			boolean service = false;

			while (clazz != null) {
				if (clazz.getCanonicalName().equals("se.rupy.http.Service")) {
					service = true;
				}
				clazz = clazz.getSuperclass();
			}

			if(service) {
				try {
					if(daemon.host) {
						final Deploy.Archive archive = this;
						Thread.currentThread().setContextClassLoader(archive);
						Service s = (Service) AccessController.doPrivileged(new PrivilegedExceptionAction() {
							public Object run() throws Exception {
								return (Service) small.clazz.newInstance();
							}
						}, access());

						this.service.add(s);
					}
					else {
						this.service.add(small.clazz.newInstance());
					}
				}
				catch(Exception e) {
					if(daemon.verbose) {
						daemon.out.println(small.name + " couldn't be instantiated!");
					}
				}
			}

			if(daemon.debug) {
				daemon.out.println(small.name + (service ? "*" : ""));
			}
		}

		static Deploy.Archive deployer = new Deploy.Archive();

		protected AccessControlContext access() {
			return access;
		}

		protected static String name(String name) {
			name = name.substring(0, name.indexOf("."));
			name = name.replace("/", ".");

			if(name.startsWith("WEB-INF.classes")) {
				name = name.substring(16);
			}

			return name;
		}

		public String name() {
			return name;
		}

		public String host() {
			return host;
		}

		public long date() {
			return date;
		}

		protected HashMap chain() {
			return chain;
		}

		protected HashSet service() {
			return service;
		}

		public String toString() {
			return name + " " + host + " " + date;
		}
	}

	static class Big implements Stream {
		private File file;
		private FileInputStream in;
		private String name;
		private long date;
		/*
		private Big(String host, String name, InputStream in, long date) throws IOException {
			file = write(host, name, in);

			this.name = name;
			this.date = date - date % 1000;
		}
		 */
		public Big(File file) {
			long date = file.lastModified();
			this.name = file.getName();
			this.file = file;
			this.date = date - date % 1000;
		}

		static File write(String host, String name, JarEntry entry, InputStream in) throws IOException {
			String path = name.substring(0, name.lastIndexOf("/"));
			String root = Deploy.path + host;

			new File(root + path).mkdirs();
			File file = new File(root + name);
			file.createNewFile();
						
			OutputStream out = new FileOutputStream(file);

			pipe(in, out);

			out.flush();
			out.close();

			file.setLastModified(entry.getTime());
			
			return file;
		}

		public String name() {
			return name;
		}

		public InputStream input() {
			try {
				in = new FileInputStream(file);
				return in;
			} catch (FileNotFoundException e) {
				return null;
			}
		}

		public void close() {
			try {
				in.close();
			} catch (IOException e) {}
		}

		public long length() {
			return file.length();
		}

		public long date() {
			return date;
		}
	}

	static class Small implements Stream {
		private String name;
		private byte[] data;
		private ByteArrayInputStream in;
		private long date;
		private Class clazz;

		public Small(String name, byte[] data) {
			this(name, data, 0);
		}

		public Small(String name, byte[] data, long date) {
			this.name = name;
			this.data = data;
			this.date = date - date % 1000;
		}

		public String name() {
			return name;
		}

		public InputStream input() {
			in = new ByteArrayInputStream(data);
			return in;
		}

		public void close() {
			try {
				in.close();
			} catch (IOException e) {}
		}

		public long length() {
			return data.length;
		}

		public long date() {
			return date;
		}

		byte[] data() {
			return data;
		}

		public String toString() {
			return name;
		}
	}

	static interface Stream {
		public String name();
		public InputStream input();
		public void close();
		public long length();
		public long date();
	}

	static class Client {
		private String cookie;

		InputStream send(URL url, File file, String pass) throws IOException {
			return send(url, file, pass, false, true);
		}

		InputStream send(URL url, File file, String pass, boolean cluster, boolean chunk) throws IOException {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			OutputStream out = null;
			InputStream in = null;

			if (file == null) {
				conn.setRequestMethod("GET");
			}
			else {
				conn.setRequestMethod("POST");

				conn.addRequestProperty("File", file.getName());
				conn.addRequestProperty("Size", "" + file.length());
				conn.addRequestProperty("Cluster", "" + cluster);
				conn.addRequestProperty("Cookie", cookie);

				if (pass != null) {
					conn.addRequestProperty("Pass", pass);
				}

				if (chunk) {
					conn.setChunkedStreamingMode(0);
				}

				conn.setDoOutput(true);

				out = conn.getOutputStream();
				in = new FileInputStream(file);

				pipe(in, out);

				out.flush();
				in.close();
			}

			int code = conn.getResponseCode();

			if(file == null) {
				cookie = conn.getHeaderField("Set-Cookie");
			}

			if (code == 200) {
				in = conn.getInputStream();
			} else if (code < 0) {
				throw new IOException("HTTP response unreadable. (" + url + ", " + file + ", " + pass + ")");
			} else {
				in = conn.getErrorStream();
			}

			return in;
		}

		public String cookie(URL url) throws IOException {
			return toString(send(url, null, null, false, false));
		}

		static String toString(InputStream in) throws IOException {
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			pipe(in, out);

			out.close();
			in.close();

			return new String(out.toByteArray());
		}

		static void toStream(InputStream in, OutputStream out) throws IOException {
			pipe(in, out);

			in.close();
		}
	}

	public static String name(String name) {
		name = name.substring(0, name.indexOf("."));
		name = name.replace("/", ".");
		return name;
	}

	public static int pipe(InputStream in, OutputStream out) throws IOException {
		return pipe(in, out, 1024, 0);
	}

	public static int pipe(InputStream in, OutputStream out, int length)
			throws IOException {
		return pipe(in, out, length, 0);
	}

	public static int pipe(InputStream in, OutputStream out, int length,
			int limit) throws IOException {
		byte[] data = new byte[length];
		int total = 0, read = in.read(data);
		while (read > -1) {
			if (limit > 0 && total > limit) {
				throw new IOException("Max allowed bytes read. (" + limit + ")");
			}
			total += read;
			out.write(data, 0, read);
			read = in.read(data);
		}
		return total;
	}

	/**
	 * Avoids the jar stream being cutoff.
	 * @author marc.larue
	 */
	static class JarInput extends JarInputStream {
		public JarInput(InputStream in) throws IOException {
			super(in);
		}

		public void close() {
			// geez
		}
	}

	public static void deploy(String host, File file, String pass) throws IOException, NoSuchAlgorithmException {
		deploy(host, file, pass, true);
	}

	/**
	 * The hash chain: file -> pass -> cookie = the man in the 
	 * middle can read your deployment file but he cannot alter, 
	 * deploy or re-deploy it!<br>
	 * <br>
	 * Basically: don't put passwords in clear text in the deployment 
	 * jar and you will be fine! To get your host, pass and database IP on <i>host.rupy.se</i>
	 * call {@link Daemon#send(Object message)} with "{"type": "account"}"
	 * like so:
<tt><br><br>
&nbsp;&nbsp;&nbsp;&nbsp;String account = (String) daemon.send("{\"type\": \"account\"}");<br>
<br></tt>
	 * It returns something like: "{"host": "xxx", "pass": "xxx", "ip": "xxx"}"
	 */
	public static String hash(File file, String pass, String cookie) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
		String hash = hash(file);
		//System.out.println(hash + " " + pass);
		hash = hash(hash + pass);
		//System.out.println(hash + " " + cookie);
		hash = hash(hash + cookie);
		//System.out.println(hash);
		return hash;
	}

	private static void deploy(String host, File file, String pass, boolean cluster) throws IOException, NoSuchAlgorithmException {
		URL url = new URL("http://" + host + "/deploy");
		Client client = new Client();
		String cookie = client.cookie(url);
		String key = hash(file, pass, cookie);
		InputStream in = client.send(url, file, key, cluster, true);
		System.out.println(new SimpleDateFormat("H:mm").format(new Date()));
		Client.toStream(in, System.out);

		// test cookie reuse hack
		//in = client.send(url, file, port, cluster, true);
	}

	private static String hash(String hash) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(hash.getBytes(), 0, hash.length());
		return new BigInteger(1, md.digest()).toString(16);
	}

	private static String hash(File file) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		InputStream in = new FileInputStream(file);
		int n = 0;
		byte[] buffer = new byte[8192];
		while (n != -1) {
			n = in.read(buffer);
			if (n > 0) {
				md.update(buffer, 0, n);
			}
		}
		return new BigInteger(1, md.digest()).toString(16);
	}

	public static void main(String[] args) {
		if (args.length > 2) {
			try {
				deploy(args[0], new File(args[1]), args[2], false);
			} catch (ConnectException ce) {
				System.out
				.println("Connection failed, is there a server running on "
						+ args[0] + "?");
			} catch (NoSuchAlgorithmException nsae) {
				System.out.println("Could not hash with SHA-256?");
			} catch (Exception e) {
				//e.printStackTrace();
			}
		} else {
			System.out.println("Usage: Deploy [host] [file] [pass]");
		}
	}
}

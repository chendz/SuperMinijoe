package se.rupy.http;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.text.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import java.nio.channels.*;

/**
 * A tiny HTTP daemon. The whole server is non-static so that you can launch
 * multiple contained HTTP servers in one application on different ports.<br>
 * <br>
 * See the configurations: {@link #Daemon()}
 * 
 * @author marc
 */

public class Daemon implements Runnable {
	static DateFormat DATE;
	
	private int selected, valid, accept, readwrite; // panel stats
	private HashMap archive, service;
	private Heart heart;
	private Selector selector;
	private String domain, name;

	Chain workers, queue;
	Properties properties;
	PrintStream out, access, error;
	AccessControlContext control, no_control;
	ConcurrentHashMap events, session;
	int threads, timeout, cookie, delay, size, port, cache;
	boolean verbose, debug, host, alive, panel;

	/**
	 * Don't forget to call {@link #start()}.
	 */
	public Daemon() {
		this(new Properties());
	}

	/**
	 * Don't forget to call {@link #start()}. The parameters below
	 * should be in the properties argument. The parenthesis contains the default value.<br><br>
	 * These are also used from the command line, so for example 'java -cp http.jar se.rupy.http.Daemon 
	 * -pass !@#$ -log -port 80' would enable remote deploy with password '!@#$', run on port 80 (requires 
	 * root on linux) and turn on logging.
	 * <table cellpadding="10">
	 * <tr><td valign="top"><b>pass</b> ()
	 * </td><td>
	 *            the pass used to deploy services with {@link Deploy} via HTTP POST, 
	 *            default is '' which disables hot-deploy; pass 'secret' only allows 
	 *            deploys from 127.0.0.1.
	 * </td></tr>
	 * <tr><td valign="top"><b>port</b> (8000)
	 * </td><td>
	 *            which TCP port.
	 * </td></tr>
	 * <tr><td valign="top"><b>threads</b> (5)
	 * </td><td>
	 *            how many worker threads, the daemon also starts one selector 
	 *            and one heartbeat thread.
	 * </td></tr>
	 * <tr><td valign="top"><b>timeout</b> (300)
	 * </td><td>
	 *            session timeout in seconds or 0 to disable sessions 
	 * </td></tr>
	 * <tr><td valign="top"><b>cookie</b> (4)</td><td>
	 *            session key character length; default and minimum is 4, > 10 can 
	 *            be considered secure.
	 * </td></tr>
	 * <tr><td valign="top"><b>delay</b> (5000)
	 * </td><td>
	 *            milliseconds before started event gets dropped due to inactivity. 
	 *            This is also the dead socket worker cleanup variable, so if 
	 *            a worker has a socket that hasn't been active for longer than 
	 *            this; the worker will be released and the socket deemed as dead.
	 * </td></tr>
	 * <tr><td valign="top"><b>size</b> (1024)</td><td>
	 *            IO buffer size in bytes, should be proportional to the data sizes 
	 *            received/sent by the server currently this is input/output- 
	 *            buffer, chunk-buffer, post-body-max and header-max lengths! :P
	 * </td></tr>
	 * <tr><td valign="top"><b>live</b> (false)
	 * </td><td>
	 *            is this rupy running live.
	 * </td></tr>
	 * <tr><td valign="top"><b>cache</b> (86400) <i>requires</i> <b>live</b>
	 * </td><td valign="top">
	 *            seconds to hard cache static files.
	 * </td></tr>
	 * <tr><td valign="top"><b>verbose</b> (false)
	 * </td><td valign="top">
	 *            to log information about these startup parameters, high-level 
	 *            info for each request and deployed services overview.
	 * </td></tr>
	 * <tr><td valign="top"><b>debug</b> (false)
	 * </td><td>
	 *            to log low-level NIO info for each request and class 
	 *            loading info.
	 * </td></tr>
	 * <tr><td valign="top"><b>log</b> (false)
	 * </td><td>
	 *            simple log of access and error in /log.
	 * </td></tr>
	 * <tr><td valign="top"><b>host</b> (false)
	 * </td><td>
	 *            to enable virtual hosting, you need to name the deployment 
	 *            jars [host].jar, for example: <i>host.rupy.se.jar</i>. 
	 *            Also if you want to deploy root domain, just deploy www.[host]; 
	 *            so for example <i>www.rupy.se.jar</i> will trigger <i>http://rupy.se</i>. 
	 *            To authenticate deployments you should use a properties file 
	 *            called <i>passport</i> in the rupy root where you store [host]=[pass].<br><br>
	 *            if your host is a <a href="http://en.wikipedia.org/wiki/Platform_as_a_service">PaaS</a> 
	 *            on <i>one machine</i>; add -Djava.security.manager -Djava.security.policy=policy 
	 *            to the rupy java process, add the passport file to your control domain 
	 *            app folder instead (for example app/host.rupy.se/passport; hide it 
	 *            from downloading with the code below) and create a symbolic link to 
	 *            that in the rupy root.
<tt><br><br>
&nbsp;&nbsp;&nbsp;&nbsp;public static class Secure extends Service {<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;public String path() { return "/passport"; }<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;public void filter(Event event) throws Event, Exception {<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;event.output().print("Nice try!");<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;}<br>
&nbsp;&nbsp;&nbsp;&nbsp;}</tt>
	 * </td></tr>
	 * <tr><td valign="top"><b>domain</b> (host.rupy.se) <i>requires</i> <b>host</b>
	 * </td><td>
	 *            if you are hosting a <a href="http://en.wikipedia.org/wiki/Platform_as_a_service">PaaS</a> 
	 *            <i>across a cluster</i>, you have to hook your control domain app up with 
	 *            {@link Daemon#set(Listener listener)}. And reply "OK" if the "auth" message authenticates with {@link Deploy#hash(File file, String pass, String cookie)}:
<tt><br><br>
&nbsp;&nbsp;&nbsp;&nbsp;{"type": "auth", "file": "[host].jar", "pass": "[pass]", "cookie": "[salt]", "cluster": [true/false]}<br>
<br></tt>
	 *            Then you can propagate the deploy with {@link Deploy#deploy(String host, File file, String pass)} 
	 *            if "cluster" is "false" in a separate thread. But for that to work you also need to answer this 
	 *            message with "OK" for your known individual cluster hosts:
<tt><br><br>
&nbsp;&nbsp;&nbsp;&nbsp;{"type": "host", "file": "[name]"}<br>
<br></tt>
	 *            Where [name] is <i>your.domain.name<b>.jar</b></i>. For example I have two 
	 *            hosts: <i>one.rupy.se</i> and <i>two.rupy.se</i> that belong under <i>host.rupy.se</i> 
	 *            so I need to return "OK" if any of these two specific domains try to deploy.
	 * </td></tr>
	 * <tr><td valign="top"><b>multi</b> (false)
	 * </td><td>
	 *            UDP multicast to all cluster nodes for real-time sync. But for this to work you also need to answer this 
	 *            message with "OK" for your known individual cluster ips:
<tt><br><br>
&nbsp;&nbsp;&nbsp;&nbsp;{"type": "packet", "from": "[ip]"}<br>
<br></tt>
	 * </td></tr>
	 * </table>
	 */
	public Daemon(Properties properties) {
		this.properties = properties;

		threads = Integer.parseInt(properties.getProperty("threads", "5"));
		cookie = Integer.parseInt(properties.getProperty("cookie", "4"));
		port = Integer.parseInt(properties.getProperty("port", "8000"));
		timeout = Integer.parseInt(properties.getProperty("timeout", "300")) * 1000;
		delay = Integer.parseInt(properties.getProperty("delay", "5000"));
		size = Integer.parseInt(properties.getProperty("size", "1024"));
		cache = Integer.parseInt(properties.getProperty("cache", "86400"));

		verbose = properties.getProperty("verbose", "false").toLowerCase()
				.equals("true");
		debug = properties.getProperty("debug", "false").toLowerCase().equals(
				"true");
		host = properties.getProperty("host", "false").toLowerCase().equals(
				"true");
		panel = properties.getProperty("panel", "false").toLowerCase().equals(
				"true");
		boolean multi = properties.getProperty("multi", "false").toLowerCase().equals(
				"true");

		if(multi) {
			try {
				setup();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}

		if(host) {
			domain = properties.getProperty("domain", "host.rupy.se");
			PermissionCollection permissions = new Permissions();
			permissions.add(new RuntimePermission("setContextClassLoader"));
			control = new AccessControlContext(new ProtectionDomain[] {
					new ProtectionDomain(null, permissions)});
			permissions = new Permissions();
			no_control = new AccessControlContext(new ProtectionDomain[] {
					new ProtectionDomain(null, permissions)});
		}

		//if (!verbose) {
		//	debug = false;
		//}

		archive = new HashMap();
		service = new HashMap();
		session = new ConcurrentHashMap();
		events = new ConcurrentHashMap();

		workers = new Chain();
		queue = new Chain();

		try {
			out = new PrintStream(System.err, true, "UTF-8");

			if(properties.getProperty("log") != null || properties.getProperty("test", "false").toLowerCase().equals(
					"true")) {
				log();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return hostname
	 */
	public String name() {
		if(name == null) {
			try {
				return InetAddress.getLocalHost().getHostName();
			}
			catch(Exception e) {
				return "unavailable";
			}
		}
		
		return name;
	}
	
	public Properties properties() {
		return properties;
	}

	protected void log() throws IOException {
		File file = new File("log");

		if(!file.exists()) {
			file.mkdir();
		}

		access = new PrintStream(new FileOutputStream(new File("log/access.txt")), true, "UTF-8");
		error = new PrintStream(new FileOutputStream(new File("log/error.txt")), true, "UTF-8");

		DATE = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");
	}

	protected void error(Event e, Throwable t) throws IOException {
		//t.printStackTrace();

		if (error != null && t != null && !(t instanceof Failure.Close)) {
			if(errlis != null) {
				if(!errlis.log(e, t))
					return;
			}
			
			Calendar date = Calendar.getInstance();
			StringBuilder b = new StringBuilder();

			b.append(DATE.format(date.getTime()));
			b.append(' ');
			b.append(e.remote());
			b.append(' ');
			b.append(e.query().path());

			String parameters = e.query().parameters();

			if(parameters != null) {
				b.append(' ');
				b.append(parameters);
			}

			b.append(Output.EOL);

			error.write(b.toString().getBytes("UTF-8"));

			t.printStackTrace(error);
		}
	}

	protected String access(Event event) throws IOException {
		if (access != null && !event.reply().push() && !event.headless) {
			Calendar date = Calendar.getInstance();
			StringBuilder b = new StringBuilder();

			b.append(DATE.format(date.getTime()));
			b.append(' ');
			b.append(event.remote());
			b.append(' ');
			b.append(event.query().path());
			b.append(' ');
			b.append(event.reply().code());

			int length = event.reply().length();

			if(length > 0) {
				b.append(' ');
				b.append(length);
			}

			return b.toString();
		}

		return null;
	}

	protected void access(String row, boolean push) throws IOException {
		if (access != null) {
			StringBuilder b = new StringBuilder();

			b.append(row);

			if(push) {
				b.append(' ');
				b.append('>');
			}

			b.append(Output.EOL);

			access.write(b.toString().getBytes("UTF-8"));
		}
	}

	/**
	 * Starts the selector, heartbeat and worker threads.
	 */
	public void start() {
		try {
			heart = new Heart();

			int threads = Integer.parseInt(properties.getProperty("threads",
					"5"));

			for (int i = 0; i < threads; i++) {
				Worker worker = new Worker(this, i);
				workers.add(worker);

				//System.err.println(worker.index() + "|" + worker.id());
			}

			alive = true;

			Thread thread = new Thread(this);
			id = thread.getId();
			thread.start();
		} catch (Exception e) {
			e.printStackTrace(out);
		}
	}

	static long id;

	/**
	 * Stops the selector, heartbeat and worker threads.
	 */
	public void stop() {
		Iterator it = workers.iterator();

		while(it.hasNext()) {
			Worker worker = (Worker) it.next();
			worker.stop();
		}

		workers.clear();
		alive = false;
		heart.stop();

		selector.wakeup();
	}

	protected ConcurrentHashMap session() {
		return session;
	}

	protected Selector selector() {
		return selector;
	}

	protected void chain(final Deploy.Archive archive) throws Exception {
		Deploy.Archive old = (Deploy.Archive) this.archive.get(archive.name());

		if (old != null) {
			Iterator it = old.service().iterator();

			while (it.hasNext()) {
				final Service service = (Service) it.next();

				try {
					if(host) {
						Thread.currentThread().setContextClassLoader(archive);
						AccessController.doPrivileged(new PrivilegedExceptionAction() {
							public Object run() throws Exception {
								service.destroy();
								return null;
							}
						}, archive.access());
					}
					else {
						service.destroy();
					}
				} catch (Exception e) {
					e.printStackTrace(out);
				}
			}
		}

		Iterator it = archive.service().iterator();

		while (it.hasNext()) {
			Service service = (Service) it.next();
			add(archive.chain(), service, archive);
		}

		this.archive.put(archive.name(), archive);
	}

	public Deploy.Archive archive(String name) {
		if(!name.endsWith(".jar")) {
			name += ".jar";
		}

		if(host) {
			if(name.equals(domain + ".jar")) {
				return Deploy.Archive.deployer;
			}

			try {
				String message = "{\"type\": \"host\", \"file\": \"" + name + "\"}";
				String ok = (String) send(message);

				if(ok.equals("OK")) {
					return Deploy.Archive.deployer;
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}

			Deploy.Archive archive = (Deploy.Archive) this.archive.get(name);

			if(archive == null) {
				return (Deploy.Archive) this.archive.get("www." + name);
			}
			else {
				return archive;
			}
		}
		else {
			return (Deploy.Archive) this.archive.get(name);
		}
	}

	private Listener listener;
	private Chain listeners;
	private ErrorListener errlis;
	
	/**
	 * Send Object to JVM listener. We recommend you only send bootclasspath loaded 
	 * classes here otherwise hotdeploy will fail.
	 * 
	 * @param message to send
	 * @return reply message
	 * @throws Exception
	 */
	public Object send(Object message) throws Exception {
		if(listener == null) {
			return message;
		}

		return listener.receive(message);
	}

	/**
	 * Intra JVM many-to-one listener. Used on cluster for domain 
	 * controller, use multicast on cluster instead.
	 * @return true if successful.
	 * @param listener
	 */
	public boolean set(Listener listener) {
		try {
			/*
			 * So only the controller can be added as listener since we use this feature to authenticate deployments.
			 */

			if(host) {
				File pass = new File("app/" + domain + "/passport");

				if(!pass.exists()) {
					pass.createNewFile();
				}

				pass.canRead();
			}

			this.listener = listener;
			return true;
		}
		catch(IOException e) {
			// if passport could not be created
		}
		
		return false;
	}

	/**
	 * Cross class-loader communication interface. So that a class deployed 
	 * in one archive can send messages to a class deployed in another.
	 * @author Marc
	 */
	public interface Listener {
		/**
		 * @param message
		 * @return the reply message to the sender.
		 * @throws Exception
		 */
		public Object receive(Object message) throws Exception;
	}

	/**
	 * Cross cluster-node communication interface. So that applications deployed 
	 * on one node can send messages to instances deployed in other nodes.
	 * @author Marc
	 */
	public interface ClusterListener {
		/**
		 * @param message the message starts with header:
		 * [host].[node], so for example; if I send a message 
		 * from cluster node <i>one</i> ({@link Daemon#name()}) 
		 * with application <i>host.rupy.se</i> the first bytes would be 
		 * 'se.rupy.host.one' followed by payload.
		 * Max length is 256 bytes!
		 * @throws Exception
		 */
		public void receive(byte[] message) throws Exception;
	}

	/**
	 * Error listener, so you can for example send a warning mail and swallow 
	 * certain exceptions to not be logged.
	 * @author Marc
	 */
	public interface ErrorListener {
		/**
		 * Here you will receive all errors before they are logged.
		 * @param e the responsible
		 * @param t the stack trace
		 * @return true if you wan't this error logged.
		 * @throws Exception
		 */
		public boolean log(Event e, Throwable t);
	}
	
	/**
	 * Listens for errors.
	 * @return true if successful.
	 * @param listener
	 */
	public boolean set(ErrorListener listener) {
		try {
			/*
			 * So only the controller can be added as error listener as the controller will mail.
			 */

			if(host) {
				File pass = new File("app/" + domain + "/passport");

				if(!pass.exists()) {
					pass.createNewFile();
				}

				pass.canRead();
			}

			this.errlis = errlis;
			return true;
		}
		catch(IOException e) {
			// if passport could not be created
		}
		
		return false;
	}
	
	/**
	 * Send inter-cluster-node UDP multicast message.
	 * @param tail your payload.
	 * Max length is 256 bytes including header: [host].[node]!
	 */
	public void broadcast(byte[] tail) throws Exception {
		if(socket != null) {
			Deploy.Archive archive = (Deploy.Archive) Thread.currentThread().getContextClassLoader();
			String name = archive.name();

			if(name == null) {
				name = domain + ".jar";
			}
			
			String[] reverse = name.split("\\.");
			StringBuilder header = new StringBuilder();

			for(int i = reverse.length - 2; i > -1; i--) {
				header.append(reverse[i]);
				
				if(i > 0) {
					header.append('.');
				}
			}
			
			header.append("." + name());
			
			byte[] head = header.toString().getBytes();
			
			if(head.length + tail.length > 256) {
				throw new Exception("Message is too long (" + header + " " + tail.length + ").");
			}

			byte[] data = new byte[head.length + tail.length];
			
			System.arraycopy(head, 0, data, 0, head.length);
			System.arraycopy(tail, 0, data, head.length, tail.length);
			
			socket.send(new DatagramPacket(data, data.length, address, 8888));
		}
	}

	/**
	 * Add multicast listener.
	 * @param listener
	 */
	public void add(ClusterListener listener) {
		if(listeners != null) {
			listeners.add(listener);
		}
	}

	/**
	 * Remove multicast listener.
	 * @param listener
	 */
	public void remove(ClusterListener listener) {
		if(listeners != null) {
			listeners.remove(listener);
		}
	}

	DatagramSocket socket;
	InetAddress address;

	private void setup() throws IOException {
		listeners = new Chain();
		address = InetAddress.getByName("224.2.2.3");
		socket = new DatagramSocket();

		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					MulticastSocket socket = new MulticastSocket(8888);
					socket.joinGroup(address);

					byte[] empty = new byte[256];
					byte[] data = new byte[256];
					DatagramPacket packet = new DatagramPacket(data, data.length);
					
					while (true) {
						socket.receive(packet);

						String message = "{\"type\": \"packet\", \"from\": \"" + packet.getAddress() + "\"}";
						String ok = (String) send(message);
						
						if(ok.equals("OK")) {
							synchronized (listeners) {
								Iterator it = listeners.iterator();

								while(it.hasNext()) {
									ClusterListener listener = (ClusterListener) it.next();
									listener.receive(data);
								}
							}
						}
						
						System.arraycopy(empty, 0, data, 0, 256);
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		});

		thread.start();
	}

	public void add(Service service) throws Exception {
		add(this.service, service, null);
	}

	protected void add(HashMap map, final Service service, final Deploy.Archive archive) throws Exception {
		String path = null;

		if(host) {
			Thread.currentThread().setContextClassLoader(archive);
			path = (String) AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					return service.path();
				}
			}, control);
		}
		else {
			path = service.path();
		}

		if(path == null) {
			path = "null";
		}

		StringTokenizer paths = new StringTokenizer(path, ":");

		while (paths.hasMoreTokens()) {
			path = paths.nextToken();
			Chain chain = (Chain) map.get(path);

			if (chain == null) {
				chain = new Chain();
				map.put(path, chain);
			}

			final Service old = (Service) chain.put(service);

			if(host) {
				final String p = path;
				Thread.currentThread().setContextClassLoader(archive);
				AccessController.doPrivileged(new PrivilegedExceptionAction() {
					public Object run() throws Exception {
						if (old != null) {
							throw new Exception(service.getClass().getName()
									+ " with path '" + p + "' and index ["
									+ service.index() + "] is conflicting with "
									+ old.getClass().getName()
									+ " for the same path and index.");
						}

						return null;
					}
				}, control);
			}
			else {
				if (old != null) {
					throw new Exception(service.getClass().getName()
							+ " with path '" + path + "' and index ["
							+ service.index() + "] is conflicting with "
							+ old.getClass().getName()
							+ " for the same path and index.");
				}
			}

			if (verbose)
				out.println(path + padding(path) + chain);

			try {
				if(host) {
					final Daemon daemon = this;
					Thread.currentThread().setContextClassLoader(archive);
					Event e = (Event) AccessController.doPrivileged(new PrivilegedExceptionAction() {
						public Object run() throws Exception {
							service.create(daemon);
							return null;
						}
					}, archive == null ? control : archive.access());
				}
				else {
					service.create(this);
				}
			} catch (Exception e) {
				e.printStackTrace(out);
			}
		}
	}

	protected String padding(String path) {
		StringBuffer buffer = new StringBuffer();

		for(int i = 0; i < 10 - path.length(); i++) {
			buffer.append(' ');
		}

		return buffer.toString();
	}

	protected void verify(final Deploy.Archive archive) throws Exception {
		Iterator it = archive.chain().keySet().iterator();

		while (it.hasNext()) {
			final String path = (String) it.next();
			Chain chain = (Chain) archive.chain().get(path);

			for (int i = 0; i < chain.size(); i++) {
				final Service service = (Service) chain.get(i);

				if(host) {
					final HashMap a = this.archive;
					final int j = i;
					Thread.currentThread().setContextClassLoader(archive);
					AccessController.doPrivileged(new PrivilegedExceptionAction() {
						public Object run() throws Exception {
							if (j != service.index()) {
								a.remove(archive.name());
								throw new Exception(service.getClass().getName()
										+ " with path '" + path + "' has index ["
										+ service.index() + "] which is too high.");
							}

							return null;
						}
					}, control);
				}
				else {
					if (i != service.index()) {
						this.archive.remove(archive.name());
						throw new Exception(service.getClass().getName()
								+ " with path '" + path + "' has index ["
								+ service.index() + "] which is too high.");
					}
				}
			}
		}
	}

	protected Deploy.Stream content(Query query) {
		if(host) {
			return content(query.header("host"), query.path());
		}
		else {
			return content(query.path());
		}
	}

	protected Deploy.Stream content(String path) {
		return content("content", path);
	}

	protected Deploy.Stream content(String host, String path) {
		if(!this.host) {
			host = "content";
		}

		File file = new File("app" + File.separator + host + File.separator + path);

		if(file.exists() && !file.isDirectory()) {
			return new Deploy.Big(file);
		}

		if(this.host) {
			file = new File("app" + File.separator + "www." + host + File.separator + path);

			if(file.exists() && !file.isDirectory()) {
				return new Deploy.Big(file);
			}
			
			try {
				String message = "{\"type\": \"host\", \"file\": \"" + host + ".jar\"}";
				String ok = (String) send(message);
				
				if(ok.equals("OK")) {
					file = new File("app" + File.separator + domain + path);

					if(file.exists() && !file.isDirectory()) {
						return new Deploy.Big(file);
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	protected Chain chain(Query query) {
		if(host) {
			return chain(query.header("host"), query.path());
		}
		else {
			return chain(query.path());
		}
	}

	public Chain chain(String path) {
		return chain("content", path);
	}

	public Chain chain(String host, String path) {
		synchronized (this.service) {
			Chain chain = (Chain) this.service.get(path);

			if (chain != null) {
				return chain;
			}
		}

		if(!this.host) {
			host = "content";
		}

		synchronized (this.archive) {
			if(this.host) {
				Deploy.Archive archive = (Deploy.Archive) this.archive.get(host + ".jar");

				if(archive == null) {
					archive = (Deploy.Archive) this.archive.get("www." + host + ".jar");
				}

				try {
					String message = "{\"type\": \"host\", \"file\": \"" + host + ".jar\"}";
					String ok = (String) send(message);

					if(ok.equals("OK")) {
						archive = (Deploy.Archive) this.archive.get(domain + ".jar");
					}
				}
				catch(Exception e) {
					e.printStackTrace();
				}
				
				if(archive != null) {
					Chain chain = (Chain) archive.chain().get(path);

					if (chain != null) {
						return chain;
					}
				}
			}
			else {
				Iterator it = this.archive.values().iterator();

				while (it.hasNext()) {
					Deploy.Archive archive = (Deploy.Archive) it.next();

					if (archive.host().equals(host)) {
						Chain chain = (Chain) archive.chain().get(path);

						if (chain != null) {
							return chain;
						}
					}
				}
			}
		}

		return null;
	}

	public void run() {
		String pass = properties.getProperty("pass", "");
		ServerSocketChannel server = null;

		try {
			selector = Selector.open();
			server = ServerSocketChannel.open();
			server.socket().bind(new InetSocketAddress(port));
			server.configureBlocking(false);
			server.register(selector, SelectionKey.OP_ACCEPT);

			DecimalFormat decimal = (DecimalFormat) DecimalFormat.getInstance();
			decimal.applyPattern("#.##");

			if (verbose) {
				boolean live = properties.getProperty("live", "false").toLowerCase().equals("true");

				out.println("daemon started\n" + "- pass       \t"
						+ pass + "\n" + "- port       \t" + port + "\n"
						+ "- worker(s)  \t" + threads + " thread"
						+ (threads > 1 ? "s" : "") + "\n" + 
						"- session    \t" + cookie + " characters\n" + 
						"- timeout    \t"
						+ decimal.format((double) timeout / 60000) + " minute"
						+ (timeout / 60000 > 1 ? "s" : "") + "\n"
						+ "- IO timeout \t" + delay + " ms." + "\n"
						+ "- IO buffer  \t" + size + " bytes\n"
						+ "- debug      \t" + debug + "\n"
						+ "- live       \t" + live
						);

				if(live)
					out.println("- cache      \t" + cache);

				out.println("- host       \t" + host);

				if(host)
					out.println("- domain     \t" + domain);
			}			

			if (pass != null && pass.length() > 0 || host) {
				if(host) {
					add(new Deploy("app" + File.separator));
				}
				else {
					add(new Deploy("app" + File.separator, pass));
				}

				File[] app = new File(Deploy.path).listFiles(new Filter());
				File domain = null;
				
				if (app != null) {
					if(host) {
						domain = new File("app" + File.separator + this.domain + ".jar");
						Deploy.deploy(this, domain, null);
					}
					
					for (int i = 0; i < app.length; i++) {
						try {
							if(host) {
								if(!app[i].getPath().equals(domain.getPath())) {
									Deploy.deploy(this, app[i], null);
								}
							}
							else {
								Deploy.deploy(this, app[i], null);
							}
						}
						catch(Error e) {
							e.printStackTrace();
						}
					}
				}
			}

			/*
			 * Used to debug thread locks and file descriptor leaks.
			 */

			if(panel) {
				add(new Service() {
					public String path() { return "/panel"; }
					public void filter(Event event) throws Event, Exception {
						Iterator it = workers.iterator();
						event.output().println("<pre>workers: {size: " + workers.size() + ", ");
						while(it.hasNext()) {
							Worker worker = (Worker) it.next();
							event.output().print(" worker: {index: " + worker.index() + ", busy: " + worker.busy() + ", lock: " + worker.lock());

							if(worker.event() != null) {
								event.output().println(", ");
								event.output().println("  event: {index: " + worker.event().index() + ", init: " + worker.event().reply().output.init + ", done: " + worker.event().reply().output.done + "}");
								event.output().println(" }");
							}
							else {
								event.output().println("}");
							}
						}
						event.output().println("}");
						event.output().println("events: {size: " + events.size() + ", selected: " + selected + ", valid: " + valid + ", accept: " + accept + ", readwrite: " + readwrite + ", ");
						it = events.values().iterator();
						while(it.hasNext()) {
							Event e = (Event) it.next();
							event.output().println(" event: {index: " + e.index() + ", push: " + e.push() + ", worker: " + (e.worker() == null ? "null" : "" + e.worker().index()) + ", last: " + (System.currentTimeMillis() - e.last()) + "}");
						}
						event.output().println("}</pre>");
					}
				});
			}

			if (properties.getProperty("test", "false").toLowerCase().equals(
					"true")) {
				new Test(this, 1);
			}
		} catch (Exception e) {
			e.printStackTrace(out);
			System.exit(1);
		}

		int index = 0;
		Event event = null;
		SelectionKey key = null;

		while (alive) {
			try {
				selector.select();

				Set set = selector.selectedKeys();
				int valid = 0, accept = 0, readwrite = 0, selected = set.size();
				Iterator it = set.iterator();

				while (it.hasNext()) {
					key = (SelectionKey) it.next();
					it.remove();

					if (key.isValid()) {
						valid++;
						if (key.isAcceptable()) {
							accept++;
							event = new Event(this, key, index++);
							events.put(new Integer(event.index()), event);

							if (Event.LOG) {
								event.log("accept ---");
							}
						} else if (key.isReadable() || key.isWritable()) {
							readwrite++;
							key.interestOps(0);

							event = (Event) key.attachment();
							Worker worker = event.worker();

							if (Event.LOG) {
								if (debug) {
									if (key.isReadable())
										event.log("read ---");
									if (key.isWritable())
										event.log("write ---");
								}
							}

							if (key.isReadable() && event.push()) {
								event.disconnect(null);
							} else if (worker == null) {
								match(event, null);
							} else {
								worker.wakeup();
							}
						}
					}
				}

				this.valid = valid;
				this.accept = accept;
				this.readwrite = readwrite;
				this.selected = selected;
			} catch (Exception e) {
				/*
				 * Here we get mostly ClosedChannelExceptions and
				 * java.io.IOException: 'Too many open files' when the server is
				 * taking a beating. Better to drop connections than to drop the
				 * server.
				 */
				if(event == null) {
					System.out.println(events + " " + key);
				}
				else {
					event.disconnect(e);
				}
			}
		}

		try {
			if(selector != null) {
				selector.close();
			}
			if(server != null) {
				server.close();
			}
		} catch (IOException e) {
			e.printStackTrace(out);
		}
	}

	private Event next() {
		if (queue.size() > 0) {
			Event event = (Event) queue.remove(0);

			while (queue.size() > 0 && event.worker() != null) {
				//System.err.print(":");
				event = (Event) queue.remove(0);
			}

			return event;
		}

		return null;
	}

	private Worker employ(Event event) {
		workers.reset();
		Worker worker = (Worker) workers.next();

		if (worker == null) {
			queue.add(event);
			return null;
		}

		while (worker.busy()) {
			worker = (Worker) workers.next();

			if (worker == null) {
				queue.add(event);
				return null;
			}
		}

		return worker;
	}

	protected synchronized boolean match(Event event, Worker worker) {
		boolean wakeup = true;

		if(event != null && worker != null) {
			event.worker(null);
			worker.event(null);

			try {
				event.register(Event.READ);
			}
			catch(CancelledKeyException e) {
				event.disconnect(e);
			}

			wakeup = false;
			event = null;
		}
		else if(event.worker() != null) {
			return false;
		}

		if(worker == null) {
			worker = employ(event);

			if(worker == null) {
				return false;
			}
		}
		else if(event == null) {
			event = next();

			if(event == null) {
				return false;
			}
			else if(event.worker() != null) {
				return event.worker() == worker;
			}
		}

		if (Event.LOG) {
			if (debug)
				out.println("event " + event.index()
						+ " and worker " + worker.index()
						+ " found each other. (" + queue.size() + ")");
		}

		worker.event(event);
		event.worker(worker);

		if(wakeup) {
			worker.wakeup();
		}

		return true;
	}

	class Filter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			if (name.endsWith(".jar")) {
				return true;
			}

			return false;
		}
	}

	protected void log(PrintStream out) {
		if(out != null) {
			this.out = out;
		}
	}

	protected void log(Object o) {
		if(out != null) {
			out.println(o);
		}
	}

	class Heart implements Runnable {
		boolean alive;

		Heart() {
			alive = true;
			new Thread(this).start();
		}

		protected void stop() {
			alive = false;
		}

		public void run() {
			int socket = 300000;

			if(timeout > 0) {
				socket = timeout;
			}

			while (alive) {
				try {
					Thread.sleep(1000);

					Iterator it = null;

					if(timeout > 0) {
						it = session.values().iterator();

						while (it.hasNext()) {
							Session se = (Session) it.next();

							if (System.currentTimeMillis() - se.date() > timeout) {
								it.remove();
								se.remove();

								if (Event.LOG) {
									if (debug)
										out.println("session timeout "
												+ se.key());
								}
							}
						}
					}

					it = workers.iterator();

					while(it.hasNext()) {
						Worker worker = (Worker) it.next();
						worker.busy();
					}

					it = events.values().iterator();

					while(it.hasNext()) {
						Event event = (Event) it.next();

						if(System.currentTimeMillis() - event.last() > socket) {
							event.disconnect(null);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(out);
				}
			}
		}
	}

	public static void main(String[] args) {
		Properties properties = new Properties();

		for (int i = 0; i < args.length; i++) {
			String flag = args[i];
			String value = null;

			if (flag.startsWith("-") && ++i < args.length) {
				value = args[i];

				if (value.startsWith("-")) {
					i--;
					value = null;
				}
			}

			if (value == null) {
				properties.put(flag.substring(1).toLowerCase(), "true");
			} else {
				properties.put(flag.substring(1).toLowerCase(), value);
			}
		}

		if (properties.getProperty("help", "false").toLowerCase()
				.equals("true")) {
			System.out.println("Usage: java -jar http.jar -verbose");
			return;
		}

		new Daemon(properties).start();

		/*
		 * If this is run as an application we log PID to pid.txt file in root.
		 */

		try {
			String pid = ManagementFactory.getRuntimeMXBean().getName();
			PrintWriter out = new PrintWriter("pid.txt");
			out.println(pid.substring(0, pid.indexOf('@')));
			out.flush();
			out.close();
		}
		catch(Exception e) {}
	}
}

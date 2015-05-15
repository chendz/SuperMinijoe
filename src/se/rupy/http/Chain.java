package se.rupy.http;

import java.io.File;
import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.AccessControlContext;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * This is the {@link Event} filter chain implementation, it has dynamic size with positional integrity.
 * @author Marc
 */
public class Chain extends LinkedList {
	private int next;

	/*
	 * Dynamic size list with positional integrity. If anyone has a better
	 * solution to this please tell me!
	 */
	protected Link put(Link link) {
		for(int i = 0; i < size(); i++) {
			Link tmp = (Link) super.get(i);

			if (link.index() == tmp.index()) {
				return (Link) set(i, link);
			}
			else if (link.index() < tmp.index()) {
				add(i, link);
				return null;
			}
		}

		add(link);

		return null;
	}

	public void filter(final Event event) throws Event, Exception {
		for (int i = 0; i < size(); i++) {
			final Service service = (Service) get(i);

			if (event.daemon().timeout > 0 && !event.headless) {
				event.session(service);
			}

			if(event.daemon().host) {
				try {
					final Deploy.Archive archive = event.daemon().archive(event.query().header("host"));
					try {
						Thread.currentThread().setContextClassLoader(archive);
					}
					catch(AccessControlException e) {
						// recursive chaining fails here, no worries! ;)
					}
					Object o = AccessController.doPrivileged(new PrivilegedExceptionAction() {
						public Object run() throws Exception {
							try {
								service.filter(event);
								return null;
							}
							catch(Event event) {
								return event;
							}
						}
					}, archive.access());

					if(o != null) {
						throw (Event) o;
					}
				}
				catch(PrivilegedActionException e) {
					if(e.getCause() != null) {
						throw (Exception) e.getCause();
					}
					else {
						throw e;
					}
				}
			}
			else {
				service.filter(event);
			}
		}
	}

	protected void exit(final Session session, final int type) throws Exception {
		for (int i = 0; i < size(); i++) {
			final Service service = (Service) get(i);

			if(session.daemon().host) {
				Thread.currentThread().setContextClassLoader(null);
				AccessController.doPrivileged(new PrivilegedExceptionAction() {
					public Object run() throws Exception {
						service.session(session, type);
						return null;
					}
				}, session.daemon().control);
			}
			else {
				service.session(session, type);
			}
		}
	}

	protected void reset() {
		next = 0;
	}

	protected Link next() {
		if (next >= size()) {
			next = 0;
			return null;
		}

		return (Link) get(next++);
	}

	interface Link {
		public int index();
	}

	public String toString() {
		StringBuilder buffer = new StringBuilder();
		Iterator it = iterator();

		buffer.append('[');

		while(it.hasNext()) {
			Object object = it.next();
			String name = object.getClass().getName();

			if(name.equals("se.rupy.http.Event")) {
				buffer.append(object);
			}
			else {
				int dollar = name.lastIndexOf('$');
				int dot = name.lastIndexOf('.');
				if(dollar > 0) {
					name = name.substring(dollar + 1);
				}
				else if(dot > 0) {
					name = name.substring(dot + 1);
				}				
				buffer.append(name);
			}

			if(it.hasNext()) {
				buffer.append(", ");
			}
		}

		buffer.append(']');

		return buffer.toString();
	}
}

package lpi.server.rest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.sun.jersey.core.util.Base64;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

public class AuthorizationFilter implements ContainerRequestFilter {

	private static final List<String> PUBLIC_RESOURCES = Arrays
			.asList(new String[] { "server/ping", "server/echo", "server/user" });

	// Exception thrown if user is unauthorized.
	private final static WebApplicationException unauthorized = new WebApplicationException(
			Response.status(Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"realm\"")
					.entity("Page requires login.").build());

	@Override
	public ContainerRequest filter(ContainerRequest containerRequest) throws WebApplicationException {

		// Automatically allow certain requests.
		String method = containerRequest.getMethod();
		String path = containerRequest.getPath(true);
		System.out.printf("%s: %s request to '%s'.%n", LocalDateTime.now(), method, path);

		// allowing access to public resources
		if (PUBLIC_RESOURCES.contains(path))
			return containerRequest;

		// Get the authentication passed in HTTP headers parameters
		String auth = containerRequest.getHeaderValue("authorization");
		if (auth == null)
			throw unauthorized;

		auth = auth.replaceFirst("[Bb]asic ", "");
		String userColonPass = Base64.base64Decode(auth);
		String[] credentials = userColonPass.split(":");

		if (credentials.length != 2 || !RestServer.get().userValid(credentials[0], credentials[1]))
			throw unauthorized;

		return containerRequest;
	}
}
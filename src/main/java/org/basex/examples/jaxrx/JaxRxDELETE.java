package org.basex.examples.jaxrx;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.basex.api.BaseXHTTP;

/**
 * This class is a simple Java client to demonstrate the JAX-RX implementation.
 * It shows the function of the HTTP DELETE method.
 *
 * @author BaseX Team 2005-11, BSD License
 */
public final class JaxRxDELETE {
  /**
   * Runs the example.
   * @throws IOException I/O exception
   */
  static void run() throws IOException {
    System.out.println("=== DELETE: delete a document or database ===");

    // The java URL connection to the resource
    URL url = new URL("http://localhost:8984/basex/jax-rx/factbook/input.xml");
    System.out.println("\n* URL: " + url);

    // Establish the connection to the URL
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    // Set as DELETE request
    conn.setRequestMethod("DELETE");

    // Print the HTTP response code
    System.out.println("\n* HTTP response: " + conn.getResponseCode() +
        " (" + conn.getResponseMessage() + ")");

    // The java URL connection to the resource
    url = new URL("http://localhost:8984/basex/jax-rx/factbook");
    System.out.println("\n* URL: " + url);

    // Establish the connection to the URL
    conn = (HttpURLConnection) url.openConnection();
    // Set as DELETE request
    conn.setRequestMethod("DELETE");

    // Print the HTTP response code
    System.out.println("\n* HTTP response: " + conn.getResponseCode() +
        " (" + conn.getResponseMessage() + ")");

    // Close connection
    conn.disconnect();
  }

  /**
   * This method demonstrates the DELETE method. In this example, an XML
   * database is deleted.
   * @param args (ignored) command-line arguments
   * @throws Exception exception
   */
  public static void main(final String... args) throws Exception {
    // Start servers
    final BaseXHTTP http = new BaseXHTTP("-W -Uadmin -Padmin");
    // Run example
    run();
    // Stop servers
    http.stop();
  }
}

package avalkiriado.environmentallabel;

import jakarta.annotation.PostConstruct;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@SpringBootApplication
@RestController
public class EnvironmentalLabelApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnvironmentalLabelApplication.class, args);
	}

	@GetMapping("/getEnvironmentalLabel")
	public String getEnvironmentalLabel(@RequestParam(value = "plate", defaultValue = "") String plate) {
		String response = request(plate);

		Document doc = Jsoup.parse(response);

		Element divElement = doc.selectFirst("#resultadoBusqueda");
		if (divElement== null) return EnvironmentalLabel.UNKNOWN.getLabel();

		Element strongElement = divElement.selectFirst("strong");
		if (strongElement == null) return EnvironmentalLabel.UNKNOWN.getLabel();
		if (divElement.text().contains("Sin distintivo")) return EnvironmentalLabel.NO_LABEL.getLabel();

		String text = strongElement.text();

		if (text.contains("Cero")) {
			return EnvironmentalLabel.CERO.getLabel();
		} else if (text.contains("Eco")) {
			return EnvironmentalLabel.ECOLOGICAL.getLabel();
		} else if (text.contains("C")) {
			return EnvironmentalLabel.C.getLabel();
		} else if (text.contains("B")) {
			return EnvironmentalLabel.B.getLabel();
		}

		return EnvironmentalLabel.UNKNOWN.getLabel();
	}

	@PostConstruct
	private static void disableCertificateVerification() {
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (Exception e) {
			System.exit(1);
		}

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setBufferRequestBody(false);
		HttpsURLConnection.setDefaultHostnameVerifier((hostname, sslSession) -> true);
	}

	private static String request(String plate) {
		RestTemplate restTemplate = new RestTemplate();
		String url = "https://sede.dgt.gob.es/es/vehiculos/distintivo-ambiental/?accion=1&matriculahd=&matricula="+plate+"&submit=Consultar";

		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

		return response.getBody();
	}

	public enum EnvironmentalLabel {
		ECOLOGICAL("ECO"),
		CERO("CERO"),
		C("C"),
		B("B"),
		NO_LABEL("NO_LABEL"),
		UNKNOWN("UNKNOWN");

		private final String label;

		EnvironmentalLabel(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}
	}

}

package de.dentrassi.camel.milo.testing;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfigBuilder;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;

import de.dentrassi.camel.milo.client.OpcUaClientComponent;
import de.dentrassi.camel.milo.server.OpcUaServerComponent;

public class Application {
	public static void main(final String[] args) throws Exception {

		// camel conext

		final CamelContext context = new DefaultCamelContext();

		// add paho

		// no need to register, gets auto detected
		// context.addComponent("paho", new PahoComponent());

		// OPC UA configuration

		final OpcUaServerConfigBuilder cfg = OpcUaServerConfig.builder();
		cfg.setCertificateManager(new DefaultCertificateManager());
		cfg.setCertificateValidator(new DefaultCertificateValidator(new File("certs")));
		cfg.setSecurityPolicies(EnumSet.of(SecurityPolicy.None));
		cfg.setIdentityValidator(new UsernameIdentityValidator(true, auth -> true));
		cfg.setUserTokenPolicies(Arrays.asList(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
				OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME));

		// add OPC UA

		context.addComponent("opcuaserver", new OpcUaServerComponent(cfg));
		context.addComponent("opcuaclient", new OpcUaClientComponent());

		// add routes

		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {
				from("paho:javaonedemo/eclipse-greenhouse-9home/sensors/temperature?brokerUrl=tcp://iot.eclipse.org:1883")
						.log("Temp update: ${body}").convertBodyTo(String.class).to("opcuaserver:MyItem");
				from("opcuaserver:MyItem").log("${body}");
				from("opcuaclient:tcp://localhost:12685/items-MyItem?namespaceUri=urn:camel").log("${body}");
			}
		});

		// start

		context.start();

		// sleep

		while (true) {
			Thread.sleep(Long.MAX_VALUE);
		}
	}
}

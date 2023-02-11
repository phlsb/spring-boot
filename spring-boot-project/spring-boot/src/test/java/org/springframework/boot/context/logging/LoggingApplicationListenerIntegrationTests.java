/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.logging;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LoggingApplicationListener}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class LoggingApplicationListenerIntegrationTests {

	@Test
	void loggingSystemRegisteredInTheContext() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SampleService.class)
				.web(WebApplicationType.NONE).run()) {
			SampleService service = context.getBean(SampleService.class);
			assertThat(service.loggingSystem).isNotNull();
		}
	}

	@Test
	void logFileRegisteredInTheContextWhenApplicable(@TempDir File tempDir) {
		String logFile = new File(tempDir, "test.log").getAbsolutePath();
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SampleService.class)
				.web(WebApplicationType.NONE).properties("logging.file.name=" + logFile).run()) {
			SampleService service = context.getBean(SampleService.class);
			assertThat(service.logFile).isNotNull();
			assertThat(service.logFile).hasToString(logFile);
		}
		finally {
			System.clearProperty(LoggingSystemProperties.LOG_FILE);
		}
	}

	@Test
	void loggingPerformedDuringChildApplicationStartIsNotLost(CapturedOutput output) {
		new SpringApplicationBuilder(Config.class).web(WebApplicationType.NONE).child(Config.class)
				.web(WebApplicationType.NONE).listeners(new ApplicationListener<ApplicationStartingEvent>() {

					private final Logger logger = LoggerFactory.getLogger(getClass());

					@Override
					public void onApplicationEvent(ApplicationStartingEvent event) {
						this.logger.info("Child application starting");
					}

				}).run();
		assertThat(output).contains("Child application starting");
	}

	@Component
	static class SampleService {

		private final LoggingSystem loggingSystem;

		private final LogFile logFile;

		SampleService(LoggingSystem loggingSystem, ObjectProvider<LogFile> logFile) {
			this.loggingSystem = loggingSystem;
			this.logFile = logFile.getIfAvailable();
		}

	}

	static class Config {

	}

}

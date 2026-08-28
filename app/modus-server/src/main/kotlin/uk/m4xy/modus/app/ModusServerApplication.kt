package uk.m4xy.modus.app

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * Modus server entry point.
 *
 * Wiring only: this class exists to start Spring and to component-scan the
 * adapters and installable modules on the runtime classpath.
 */
@SpringBootApplication(scanBasePackages = ["uk.m4xy.modus"])
public class ModusServerApplication

public fun main(args: Array<String>) {
    SpringApplication.run(ModusServerApplication::class.java, *args)
}

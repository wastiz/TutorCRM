package com.tutormgmt.dev;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds demo data when the app starts with {@code SPRING_PROFILES_ACTIVE=local}.
 *
 * <p>By default it only fills an empty demo account, so restarting after a code change keeps
 * whatever you clicked together last time. Set {@code app.dev.reset-on-startup=true} (or call
 * {@code POST /dev/seed?reset=true}) to get a clean, freshly dated dataset instead.
 */
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class DevDataRunner implements ApplicationRunner {

    private final DevProperties props;
    private final DevDataSeeder seeder;

    @Override
    public void run(ApplicationArguments args) {
        if (!props.seedOnStartup() && !props.resetOnStartup()) {
            return;
        }
        DevSeedResult result = seeder.seed(props.resetOnStartup());
        log.info("Dev data ready for {} — {} students, {} lessons. Sign in at /dev/login",
                result.email(), result.students(), result.lessons());
    }
}

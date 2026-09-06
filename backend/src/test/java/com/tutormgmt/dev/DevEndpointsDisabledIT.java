package com.tutormgmt.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tutormgmt.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Without the {@code local} profile — i.e. in every deployed environment — the dev sign-in and the
 * seeder must not exist at all. This test runs under the default profile on purpose.
 */
@AutoConfigureMockMvc
class DevEndpointsDisabledIT extends AbstractPostgresIT {

    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;

    @Test
    void theSeederAndItsRoutesAreNotEvenRegistered() {
        assertThat(context.getBeanNamesForType(DevDataSeeder.class)).isEmpty();
        assertThat(context.getBeanNamesForType(DevAuthController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(DevDataRunner.class)).isEmpty();
    }

    @Test
    void devSignInIsNotReachable() throws Exception {
        mvc.perform(get("/dev/login")).andExpect(status().isUnauthorized());
        mvc.perform(post("/dev/seed")).andExpect(status().isUnauthorized());
    }
}

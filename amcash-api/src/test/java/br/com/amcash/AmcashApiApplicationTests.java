package br.com.amcash;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AmcashApiApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void shouldReturnUnauthorizedWithoutJwt() throws Exception {
		mockMvc.perform(get("/api/v1/transactions")
					.param("year", "2026")
					.param("month", "9"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Autenticação necessária"));
	}

}

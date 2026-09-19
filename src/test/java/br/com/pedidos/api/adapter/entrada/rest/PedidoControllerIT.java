package br.com.pedidos.api.adapter.entrada.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PedidoControllerIT {

    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":\"([0-9a-fA-F-]{36})\"");

    @Autowired
    private MockMvc mockMvc;

    private static String extrairId(String corpo) {
        Matcher matcher = ID_PATTERN.matcher(corpo);
        if (!matcher.find()) {
            throw new IllegalStateException("id não encontrado na resposta: " + corpo);
        }
        return matcher.group(1);
    }

    @Test
    void criaPedidoComItemValido() throws Exception {
        mockMvc.perform(post("/pedidos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "clienteId": "c-1",
                                  "itens": [
                                    { "sku": "CAFE-500", "quantidade": 2, "precoUnitario": 18.90 }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.total").value(37.80));
    }

    @Test
    void recusaQuantidadeZeroCom422() throws Exception {
        mockMvc.perform(post("/pedidos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "clienteId": "c-1",
                                  "itens": [
                                    { "sku": "CAFE-500", "quantidade": 0, "precoUnitario": 18.90 }
                                  ]
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void recusaItensVaziosCom422() throws Exception {
        mockMvc.perform(post("/pedidos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "clienteId": "c-1",
                                  "itens": []
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void recusaJsonMalformadoCom400() throws Exception {
        mockMvc.perform(post("/pedidos")
                        .contentType("application/json")
                        .content("{ isso nao e json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criaPedidoEDepoisAdicionaItemAtualizandoTotal() throws Exception {
        MvcResult criacao = mockMvc.perform(post("/pedidos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "clienteId": "c-1",
                                  "itens": [
                                    { "sku": "CAFE-500", "quantidade": 2, "precoUnitario": 18.90 }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String id = extrairId(criacao.getResponse().getContentAsString());

        mockMvc.perform(post("/pedidos/" + id + "/itens")
                        .contentType("application/json")
                        .content("""
                                { "sku": "CAFE-500", "quantidade": 1, "precoUnitario": 18.90 }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.total").value(56.70));
    }

    @Test
    void adicionarItemComQuantidadeInvalidaRetorna422() throws Exception {
        MvcResult criacao = mockMvc.perform(post("/pedidos")
                        .contentType("application/json")
                        .content("""
                                {
                                  "clienteId": "c-1",
                                  "itens": [
                                    { "sku": "CAFE-500", "quantidade": 2, "precoUnitario": 18.90 }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String id = extrairId(criacao.getResponse().getContentAsString());

        mockMvc.perform(post("/pedidos/" + id + "/itens")
                        .contentType("application/json")
                        .content("""
                                { "sku": "CAFE-500", "quantidade": 0, "precoUnitario": 18.90 }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void adicionarItemEmPedidoInexistenteRetorna404() throws Exception {
        mockMvc.perform(post("/pedidos/" + UUID.randomUUID() + "/itens")
                        .contentType("application/json")
                        .content("""
                                { "sku": "CAFE-500", "quantidade": 1, "precoUnitario": 18.90 }
                                """))
                .andExpect(status().isNotFound());
    }
}

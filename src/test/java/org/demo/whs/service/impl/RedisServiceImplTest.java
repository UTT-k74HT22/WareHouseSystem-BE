package org.demo.whs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisServiceImpl Unit Tests")
class RedisServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RedisServiceImpl redisService;

    @Nested
    @DisplayName("set and get tests")
    class SetAndGetTests {

        @Test
        @DisplayName("should_SetKeyWithTtlSuccessfully")
        void should_SetKeyWithTtlSuccessfully() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doNothing().when(valueOperations).set("key1", "val1", 10L, TimeUnit.MINUTES);

            redisService.set("key1", "val1", 10L, TimeUnit.MINUTES);

            verify(valueOperations).set("key1", "val1", 10L, TimeUnit.MINUTES);
        }

        @Test
        @DisplayName("should_ThrowRuntimeException_When_SetFails")
        void should_ThrowRuntimeException_When_SetFails() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doThrow(new RuntimeException("Redis connection refused"))
                    .when(valueOperations).set(any(), any(), anyLong(), any());

            assertThatThrownBy(() -> redisService.set("key1", "val1", 10L, TimeUnit.MINUTES))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to set Redis key");
        }

        @Test
        @DisplayName("should_GetValueSuccessfully")
        void should_GetValueSuccessfully() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("key1")).thenReturn("val1");

            Object result = redisService.get("key1");

            assertThat(result).isEqualTo("val1");
        }

        @Test
        @DisplayName("should_ThrowRuntimeException_When_GetFails")
        void should_ThrowRuntimeException_When_GetFails() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("errKey")).thenThrow(new RuntimeException("Redis timeout"));

            assertThatThrownBy(() -> redisService.get("errKey"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to get Redis key");
        }
    }

    @Nested
    @DisplayName("get with Class tests")
    class GetWithClassTests {

        @Test
        @DisplayName("should_ReturnEmptyOptional_When_ValueIsNull")
        void should_ReturnEmptyOptional_When_ValueIsNull() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("missing")).thenReturn(null);

            Optional<String> result = redisService.get("missing", String.class);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_ReturnValueDirectly_When_TypeMatches")
        void should_ReturnValueDirectly_When_TypeMatches() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("numKey")).thenReturn(123);

            Optional<Integer> result = redisService.get("numKey", Integer.class);

            assertThat(result).contains(123);
        }

        @Test
        @DisplayName("should_DeserializeJsonString_When_TargetIsNotString")
        void should_DeserializeJsonString_When_TargetIsNotString() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("jsonKey")).thenReturn("{\"id\":\"acc-1\",\"name\":\"John\"}");

            record TestUser(String id, String name) {}

            Optional<TestUser> result = redisService.get("jsonKey", TestUser.class);

            assertThat(result).isPresent();
            assertThat(result.get().id()).isEqualTo("acc-1");
            assertThat(result.get().name()).isEqualTo("John");
        }

        @Test
        @DisplayName("should_ReturnEmptyOptional_When_DeserializationThrows")
        void should_ReturnEmptyOptional_When_DeserializationThrows() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("badJson")).thenReturn("not valid json");

            record TestUser(String id, String name) {}

            Optional<TestUser> result = redisService.get("badJson", TestUser.class);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("exists and delete tests")
    class ExistsAndDeleteTests {

        @Test
        @DisplayName("should_ReturnTrue_When_KeyExists")
        void should_ReturnTrue_When_KeyExists() {
            when(redisTemplate.hasKey("existKey")).thenReturn(true);

            assertThat(redisService.exists("existKey")).isTrue();
        }

        @Test
        @DisplayName("should_ReturnFalse_When_KeyDoesNotExist")
        void should_ReturnFalse_When_KeyDoesNotExist() {
            when(redisTemplate.hasKey("noKey")).thenReturn(false);

            assertThat(redisService.exists("noKey")).isFalse();
        }

        @Test
        @DisplayName("should_ReturnFalse_When_ExistsThrowsException")
        void should_ReturnFalse_When_ExistsThrowsException() {
            when(redisTemplate.hasKey("errKey")).thenThrow(new RuntimeException("Redis down"));

            assertThat(redisService.exists("errKey")).isFalse();
        }

        @Test
        @DisplayName("should_DeleteKeySuccessfully")
        void should_DeleteKeySuccessfully() {
            when(redisTemplate.delete("delKey")).thenReturn(true);

            redisService.delete("delKey");

            verify(redisTemplate).delete("delKey");
        }

        @Test
        @DisplayName("should_ThrowRuntimeException_When_DeleteFails")
        void should_ThrowRuntimeException_When_DeleteFails() {
            when(redisTemplate.delete("errDel")).thenThrow(new RuntimeException("Redis down"));

            assertThatThrownBy(() -> redisService.delete("errDel"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to delete Redis key");
        }
    }

    @Nested
    @DisplayName("getOptional with TypeReference tests")
    class GetOptionalTypeReferenceTests {

        @Test
        @DisplayName("should_ReturnEmpty_When_CacheMiss")
        void should_ReturnEmpty_When_CacheMiss() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("missKey")).thenReturn(null);

            Optional<List<String>> result = redisService.getOptional("missKey", new TypeReference<>() {});

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_DeserializeJsonStringSuccessfully")
        void should_DeserializeJsonStringSuccessfully() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("rolesKey")).thenReturn("[\"ADMIN\",\"USER\"]");

            Optional<List<String>> result = redisService.getOptional("rolesKey", new TypeReference<>() {});

            assertThat(result).isPresent();
            assertThat(result.get()).containsExactly("ADMIN", "USER");
        }

        @Test
        @DisplayName("should_ConvertNonStringObjectSuccessfully")
        void should_ConvertNonStringObjectSuccessfully() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("rawList")).thenReturn(List.of("READ", "WRITE"));

            Optional<List<String>> result = redisService.getOptional("rawList", new TypeReference<>() {});

            assertThat(result).isPresent();
            assertThat(result.get()).containsExactly("READ", "WRITE");
        }

        @Test
        @DisplayName("should_ReturnEmpty_When_DeserializationFails")
        void should_ReturnEmpty_When_DeserializationFails() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("corruptKey")).thenReturn("{corrupt json");

            Optional<List<String>> result = redisService.getOptional("corruptKey", new TypeReference<>() {});

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveWithTTL tests")
    class SaveWithTTLTests {

        @Test
        @DisplayName("should_Skip_When_KeyOrValueIsNull")
        void should_Skip_When_KeyOrValueIsNull() {
            redisService.saveWithTTL(null, "val", 1L, TimeUnit.HOURS);
            redisService.saveWithTTL("key", null, 1L, TimeUnit.HOURS);

            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("should_SaveSerializedJsonWithTTLSuccessfully")
        void should_SaveSerializedJsonWithTTLSuccessfully() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doNothing().when(valueOperations).set(eq("key1"), anyString(), eq(2L), eq(TimeUnit.HOURS));

            redisService.saveWithTTL("key1", List.of("A", "B"), 2L, TimeUnit.HOURS);

            verify(valueOperations).set(eq("key1"), contains("[\"A\",\"B\"]"), eq(2L), eq(TimeUnit.HOURS));
        }

        @Test
        @DisplayName("should_ThrowRuntimeException_When_SaveThrowsException")
        void should_ThrowRuntimeException_When_SaveThrowsException() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doThrow(new RuntimeException("Save error")).when(valueOperations).set(any(), any(), anyLong(), any());

            assertThatThrownBy(() -> redisService.saveWithTTL("key1", "val1", 1L, TimeUnit.HOURS))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to save Redis key");
        }
    }

    @Nested
    @DisplayName("getAllKeys tests")
    class GetAllKeysTests {

        @Test
        @DisplayName("should_ReturnEmptySet_When_PatternIsNullOrBlank")
        void should_ReturnEmptySet_When_PatternIsNullOrBlank() {
            assertThat(redisService.getAllKeys(null)).isEmpty();
            assertThat(redisService.getAllKeys("   ")).isEmpty();
            verify(redisTemplate, never()).keys(anyString());
        }

        @Test
        @DisplayName("should_ReturnKeys_When_PatternMatches")
        void should_ReturnKeys_When_PatternMatches() {
            when(redisTemplate.keys("prefix:*")).thenReturn(Set.of("prefix:1", "prefix:2"));

            Set<String> result = redisService.getAllKeys("prefix:*");

            assertThat(result).containsExactlyInAnyOrder("prefix:1", "prefix:2");
        }

        @Test
        @DisplayName("should_ReturnEmptySet_When_KeysReturnsNull")
        void should_ReturnEmptySet_When_KeysReturnsNull() {
            when(redisTemplate.keys("empty:*")).thenReturn(null);

            Set<String> result = redisService.getAllKeys("empty:*");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_ReturnEmptySet_When_KeysThrowsException")
        void should_ReturnEmptySet_When_KeysThrowsException() {
            when(redisTemplate.keys("err:*")).thenThrow(new RuntimeException("Cluster error"));

            Set<String> result = redisService.getAllKeys("err:*");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete Collection tests")
    class DeleteCollectionTests {

        @Test
        @DisplayName("should_Skip_When_CollectionIsNullorEmpty")
        void should_Skip_When_CollectionIsNullorEmpty() {
            redisService.delete((Collection<String>) null);
            redisService.delete(Collections.emptyList());

            verify(redisTemplate, never()).delete(anyCollection());
        }

        @Test
        @DisplayName("should_DeleteCollectionSuccessfully")
        void should_DeleteCollectionSuccessfully() {
            when(redisTemplate.delete(anyCollection())).thenReturn(3L);

            redisService.delete(List.of("k1", "k2", "k3"));

            verify(redisTemplate).delete(List.of("k1", "k2", "k3"));
        }

        @Test
        @DisplayName("should_NotThrow_When_DeleteCollectionThrowsException")
        void should_NotThrow_When_DeleteCollectionThrowsException() {
            when(redisTemplate.delete(anyCollection())).thenThrow(new RuntimeException("Redis error"));

            // Must catch silently and log error
            redisService.delete(List.of("k1"));

            verify(redisTemplate).delete(anyCollection());
        }
    }
}

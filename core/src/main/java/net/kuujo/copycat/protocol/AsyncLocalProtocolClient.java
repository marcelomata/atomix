/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kuujo.copycat.protocol;

import net.kuujo.copycat.spi.protocol.AsyncProtocolClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Local protocol client.
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public class AsyncLocalProtocolClient implements AsyncProtocolClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncLocalProtocolClient.class);
  private final String id;
  private final Map<String, AsyncLocalProtocolServer> registry;

  public AsyncLocalProtocolClient(String id, Map<String, AsyncLocalProtocolServer> registry) {
    this.id = id;
    this.registry = registry;
  }

  @Override
  public CompletableFuture<PingResponse> ping(PingRequest request) {
    AsyncLocalProtocolServer server = registry.get(id);
    if (server == null) {
      CompletableFuture<PingResponse> future = new CompletableFuture<>();
      future.completeExceptionally(new ProtocolException("Invalid server address: %s", id));
      return future;
    }
    return server.ping(request);
  }

  @Override
  public CompletableFuture<SyncResponse> sync(SyncRequest request) {
    AsyncLocalProtocolServer server = registry.get(id);
    if (server == null) {
      CompletableFuture<SyncResponse> future = new CompletableFuture<>();
      future.completeExceptionally(new ProtocolException("Invalid server address: %s", id));
      return future;
    }
    return server.sync(request);
  }

  @Override
  public CompletableFuture<PollResponse> poll(PollRequest request) {
    AsyncLocalProtocolServer server = registry.get(id);
    if (server == null) {
      CompletableFuture<PollResponse> future = new CompletableFuture<>();
      future.completeExceptionally(new ProtocolException("Invalid server address: %s", id));
      return future;
    }
    return server.poll(request);
  }

  @Override
  public CompletableFuture<SubmitResponse> submit(SubmitRequest request) {
    AsyncLocalProtocolServer server = registry.get(id);
    if (server == null) {
      CompletableFuture<SubmitResponse> future = new CompletableFuture<>();
      future.completeExceptionally(new ProtocolException("Invalid server address: %s", id));
      return future;
    }
    return server.submit(request);
  }

  @Override
  public CompletableFuture<Void> connect() {
    LOGGER.debug("{} connecting to {}", this, id);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> close() {
    LOGGER.debug("{} closing connection to {}", this, id);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public String toString() {
    return String.format("%s[id=%s]", getClass().getSimpleName(), id);
  }

}

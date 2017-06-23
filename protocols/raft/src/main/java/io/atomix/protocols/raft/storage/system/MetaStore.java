/*
 * Copyright 2015-present Open Networking Laboratory
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
 * limitations under the License
 */
package io.atomix.protocols.raft.storage.system;

import io.atomix.logging.Logger;
import io.atomix.logging.LoggerFactory;
import io.atomix.protocols.raft.cluster.MemberId;
import io.atomix.protocols.raft.storage.RaftStorage;
import io.atomix.serializer.Serializer;
import io.atomix.storage.StorageLevel;
import io.atomix.storage.buffer.Buffer;
import io.atomix.storage.buffer.FileBuffer;
import io.atomix.storage.buffer.HeapBuffer;

import java.io.File;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages persistence of server configurations.
 * <p>
 * The server metastore is responsible for persisting server configurations according to the configured
 * {@link RaftStorage#getStorageLevel() storage level}. Each server persists their current {@link #loadTerm() term}
 * and last {@link #loadVote() vote} as is dictated by the Raft consensus algorithm. Additionally, the
 * metastore is responsible for storing the last know server {@link Configuration}, including cluster
 * membership.
 */
public class MetaStore implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetaStore.class);
  private final Serializer serializer;
  private final FileBuffer metadataBuffer;
  private final Buffer configurationBuffer;

  public MetaStore(RaftStorage storage, Serializer serializer) {
    this.serializer = checkNotNull(serializer, "serializer cannot be null");

    if (!(storage.getDirectory().isDirectory() || storage.getDirectory().mkdirs())) {
      throw new IllegalArgumentException(String.format("Can't create storage directory [%s].", storage.getDirectory()));
    }

    // Note that for raft safety, irrespective of the storage level, <term, vote> metadata is always persisted on disk.
    File metaFile = new File(storage.getDirectory(), String.format("%s.meta", storage.getPrefix()));
    metadataBuffer = FileBuffer.allocate(metaFile, 12);

    if (storage.getStorageLevel() == StorageLevel.MEMORY) {
      configurationBuffer = HeapBuffer.allocate(32);
    } else {
      File confFile = new File(storage.getDirectory(), String.format("%s.conf", storage.getPrefix()));
      configurationBuffer = FileBuffer.allocate(confFile, 32);
    }
  }

  /**
   * Stores the current server term.
   *
   * @param term The current server term.
   */
  public synchronized void storeTerm(long term) {
    LOGGER.trace("Store term {}", term);
    metadataBuffer.writeLong(0, term).flush();
  }

  /**
   * Loads the stored server term.
   *
   * @return The stored server term.
   */
  public synchronized long loadTerm() {
    return metadataBuffer.readLong(0);
  }

  /**
   * Stores the last voted server.
   *
   * @param vote The server vote.
   */
  public synchronized void storeVote(MemberId vote) {
    LOGGER.trace("Store vote {}", vote);
    metadataBuffer.writeString(8, vote != null ? vote.id() : null).flush();
  }

  /**
   * Loads the last vote for the server.
   *
   * @return The last vote for the server.
   */
  public synchronized MemberId loadVote() {
    String id = metadataBuffer.readString(8);
    return id != null ? MemberId.memberId(id) : null;
  }

  /**
   * Stores the current cluster configuration.
   *
   * @param configuration The current cluster configuration.
   */
  public synchronized void storeConfiguration(Configuration configuration) {
    LOGGER.trace("Store configuration {}", configuration);
    byte[] bytes = serializer.encode(configuration);
    configurationBuffer.position(0)
        .writeByte(1)
        .writeInt(bytes.length)
        .write(bytes);
    configurationBuffer.flush();
  }

  /**
   * Loads the current cluster configuration.
   *
   * @return The current cluster configuration.
   */
  public synchronized Configuration loadConfiguration() {
    if (configurationBuffer.position(0).readByte() == 1) {
      return serializer.decode(configurationBuffer.readBytes(configurationBuffer.readInt()));
    }
    return null;
  }

  @Override
  public synchronized void close() {
    metadataBuffer.close();
    configurationBuffer.close();
  }

  @Override
  public String toString() {
    return toStringHelper(this).toString();
  }

}

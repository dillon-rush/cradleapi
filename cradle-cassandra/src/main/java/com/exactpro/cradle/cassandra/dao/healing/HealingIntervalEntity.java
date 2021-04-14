/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.cradle.cassandra.dao.healing;

import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.exactpro.cradle.healing.HealingInterval;
import com.exactpro.cradle.healing.RecoveryState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalTime;
import java.util.UUID;

import static com.exactpro.cradle.cassandra.StorageConstants.HEALING_INTERVAL_END_TIME;
import static com.exactpro.cradle.cassandra.StorageConstants.HEALING_INTERVAL_ID;
import static com.exactpro.cradle.cassandra.StorageConstants.HEALING_INTERVAL_START_TIME;
import static com.exactpro.cradle.cassandra.StorageConstants.INSTANCE_ID;
import static com.exactpro.cradle.cassandra.StorageConstants.RECOVERY_STATE_JSON;

@Entity
public class HealingIntervalEntity
{
    private static final Logger logger = LoggerFactory.getLogger(HealingIntervalEntity.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @PartitionKey(0)
    @CqlName(INSTANCE_ID)
    private UUID instanceId;

    @PartitionKey(1)
    @CqlName(HEALING_INTERVAL_ID)
    private String healingIntervalId;

    @CqlName(HEALING_INTERVAL_START_TIME)
    private LocalTime startTime;

    @CqlName(HEALING_INTERVAL_END_TIME)
    private LocalTime endTime;

    @CqlName(RECOVERY_STATE_JSON)
    private String recoveryStateJson;

    public HealingIntervalEntity()
    {
    }

    public HealingIntervalEntity(HealingInterval interval, UUID instanceId)
    {
        this.healingIntervalId = interval.getId();
        this.startTime = interval.getStartTime();
        this.endTime = interval.getEndTime();

        try {
            this.recoveryStateJson = mapper.writeValueAsString(interval.getRecoveryState());
        } catch (JsonProcessingException e) {
            logger.error("Error while converting recovery state to JSON format", e);
        }

        this.instanceId = instanceId;
    }

    public UUID getInstanceId()
    {
        return instanceId;
    }

    public void setInstanceId(UUID instanceId)
    {
        this.instanceId = instanceId;
    }

    public String getHealingIntervalId() { return healingIntervalId; }

    public void setHealingIntervalId(String healingIntervalId) { this.healingIntervalId = healingIntervalId; }

    public LocalTime getStartTime() { return startTime; }

    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }

    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getRecoveryStateJson() { return recoveryStateJson; }

    public void setRecoveryStateJson(String recoveryStateJson) { this.recoveryStateJson = recoveryStateJson; }

    public HealingInterval asHealingInterval() throws IOException {
        return new HealingInterval(this.healingIntervalId, this.startTime, this.endTime, mapper.readValue(recoveryStateJson, RecoveryState.class));
    }
}

/*
 * Copyright (C) 2017 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.vsct.tock.nlp.front.shared.config

import fr.vsct.tock.nlp.core.Entity
import fr.vsct.tock.nlp.core.EntityType
import fr.vsct.tock.nlp.core.Intent
import fr.vsct.tock.nlp.core.sample.SampleContext
import fr.vsct.tock.nlp.core.sample.SampleEntity
import fr.vsct.tock.nlp.core.sample.SampleExpression
import fr.vsct.tock.nlp.front.shared.parser.ParseResult
import java.time.Instant
import java.util.Locale

/**
 *
 */
data class ClassifiedSentence(val text: String,
                              val language: Locale,
                              val applicationId: String,
                              val creationDate: Instant,
                              val updateDate: Instant,
                              val status: ClassifiedSentenceStatus,
                              val classification: Classification,
                              val lastIntentProbability: Double?,
                              val lastEntityProbability: Double?) {

    constructor(
            query: ParseResult,
            language: Locale,
            applicationId: String,
            intentId: String,
            lastIntentProbability: Double,
            lastEntityProbability: Double)
            : this(
            query.retainedQuery,
            language,
            applicationId,
            Instant.now(),
            Instant.now(),
            ClassifiedSentenceStatus.inbox,
            Classification(query, intentId),
            lastIntentProbability,
            lastEntityProbability
    )

    /**
     * Check if the sentence has the same content (status, creation & update dates excluded)
     */
    fun hasSameContent(sentence: ClassifiedSentence?): Boolean {
        return this == sentence?.copy(
                status = status,
                creationDate = creationDate,
                updateDate = updateDate,
                lastIntentProbability = lastIntentProbability,
                lastEntityProbability = lastEntityProbability)
    }

    /**
     * Build an expression from this sentence.
     *
     * @param intentProvider intent id -> intent provider
     * @param entityTypeProvider entity type name -> entity type provider
     */
    fun toSampleExpression(intentProvider: (String) -> Intent, entityTypeProvider: (String) -> EntityType): SampleExpression {
        return SampleExpression(
                text,
                intentProvider.invoke(classification.intentId),
                classification.entities.map {
                    toSampleEntity(it, entityTypeProvider)
                },
                SampleContext(language))
    }

    private fun toSampleEntity(entity: ClassifiedEntity, entityTypeProvider: (String) -> EntityType): SampleEntity {
        val entityType = entityTypeProvider.invoke(entity.type)
        return SampleEntity(
                Entity(entityType, entity.role),
                entity.subEntities.map { toSampleEntity(it, entityTypeProvider) },
                entity.start,
                entity.end)
    }
}
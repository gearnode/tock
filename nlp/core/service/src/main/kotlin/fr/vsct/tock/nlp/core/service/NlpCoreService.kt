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

package fr.vsct.tock.nlp.core.service

import com.github.salomonbrys.kodein.instance
import fr.vsct.tock.nlp.core.CallContext
import fr.vsct.tock.nlp.core.Entity
import fr.vsct.tock.nlp.core.EntityRecognition
import fr.vsct.tock.nlp.core.EntityType
import fr.vsct.tock.nlp.core.Intent
import fr.vsct.tock.nlp.core.IntentClassification
import fr.vsct.tock.nlp.core.IntentSelector
import fr.vsct.tock.nlp.core.NlpCore
import fr.vsct.tock.nlp.core.NlpEngineType
import fr.vsct.tock.nlp.core.ParsingResult
import fr.vsct.tock.nlp.core.merge.ValueDescriptor
import fr.vsct.tock.nlp.core.quality.TestContext
import fr.vsct.tock.nlp.core.service.entity.EntityCore
import fr.vsct.tock.nlp.core.service.entity.EntityMerge
import fr.vsct.tock.nlp.model.EntityCallContextForEntity
import fr.vsct.tock.nlp.model.EntityCallContextForIntent
import fr.vsct.tock.nlp.model.IntentContext
import fr.vsct.tock.nlp.model.ModelHolder
import fr.vsct.tock.nlp.model.ModelNotInitializedException
import fr.vsct.tock.nlp.model.NlpClassifier
import fr.vsct.tock.nlp.model.TokenizerContext
import fr.vsct.tock.shared.error
import fr.vsct.tock.shared.injector
import mu.KotlinLogging

/**
 *
 */
object NlpCoreService : NlpCore {

    private val logger = KotlinLogging.logger {}

    private val unknownResult = ParsingResult(Intent.Companion.UNKNOWN_INTENT, emptyList(), 1.0, 1.0)

    private val entityCore: EntityCore by injector.instance()
    private val entityMerge: EntityMerge by injector.instance()
    private val nlpClassifier: NlpClassifier by injector.instance()

    override fun parse(context: CallContext,
                       text: String,
                       intentSelector: IntentSelector): ParsingResult {
        return parse(
                context,
                text,
                { tokens -> nlpClassifier.classifyIntent(IntentContext(context), text, tokens) },
                { intent, tokens -> nlpClassifier.classifyEntities(EntityCallContextForIntent(context, intent), text, tokens) },
                intentSelector
        )
    }

    internal fun parse(context: TestContext,
                       text: String,
                       intentModelHolder: ModelHolder,
                       entityModelHolders: Map<Intent, ModelHolder?>): ParsingResult {
        return parse(
                context.callContext,
                text,
                { tokens -> nlpClassifier.classifyIntent(IntentContext(context), intentModelHolder, text, tokens) },
                { intent, tokens ->
                    nlpClassifier.classifyEntities(
                            EntityCallContextForIntent(context, intent),
                            entityModelHolders[intent],
                            text,
                            tokens)
                },
                IntentSelector.defaultIntentSelector
        )
    }

    private fun parse(callContext: CallContext,
                      text: String,
                      intentClassifier: (Array<String>) -> IntentClassification,
                      entityClassifier: (Intent, Array<String>) -> List<EntityRecognition>,
                      intentSelector: IntentSelector): ParsingResult {
        try {
            val tokens = nlpClassifier.tokenize(TokenizerContext(callContext), text)
            val intents = intentClassifier.invoke(tokens)
            val (intent, probability) = intentSelector.selectIntent(intents) ?: null to null

            if (intent == null || probability == null) {
                return unknownResult
            }

            val evaluatedEntities = classifyAndEvaluate(callContext, intent, entityClassifier, text, tokens)

            return ParsingResult(
                    intent.name,
                    evaluatedEntities,
                    probability,
                    if (evaluatedEntities.isEmpty()) 1.0 else evaluatedEntities.map { it.probability }.average())
        } catch (e: ModelNotInitializedException) {
            logger.warn { "model not initialized : ${e.message}" }
            return unknownResult
        } catch (e: Exception) {
            logger.error(e)
            return unknownResult
        }
    }

    private fun classifyAndEvaluate(
            context: CallContext,
            intent: Intent,
            entityClassifier: (Intent, Array<String>) -> List<EntityRecognition>,
            text: String,
            tokens: Array<String>): List<EntityRecognition> {
        return try {
            //TODO regexp

            //evaluate entities from intent entity model & dedicated entity models
            val intentContext = EntityCallContextForIntent(context, intent)
            val entities = entityClassifier.invoke(intent, tokens)
            val evaluatedEntities = evaluateEntities(context, text, entities)

            return if (context.evaluationContext.mergeEntityTypes) {
                val classifiedEntityTypes = entityCore.classifyEntityTypes(intentContext, text, tokens)
                entityMerge.mergeEntityTypes(context, text, intent, evaluatedEntities, classifiedEntityTypes)
            } else {
                evaluatedEntities
            }
        } catch (e: Exception) {
            logger.error(e)
            emptyList()
        }
    }

    override fun evaluateEntities(
            context: CallContext,
            text: String,
            entities: List<EntityRecognition>): List<EntityRecognition> {
        return entityCore.evaluateEntities(context, text, entities)
    }

    override fun supportedNlpEngineTypes(): Set<NlpEngineType> {
        return nlpClassifier.supportedNlpEngineTypes()
    }

    override fun getEvaluableEntityTypes(): Set<String> {
        return entityCore.getEvaluableEntityTypes()
    }

    override fun supportValuesMerge(entityType: EntityType): Boolean {
        return entityCore.supportValuesMerge(entityType)
    }

    override fun mergeValues(context: CallContext, entity: Entity, values: List<ValueDescriptor>): ValueDescriptor? {
        return entityCore.mergeValues(EntityCallContextForEntity(context, entity), values)
    }

    override fun healthcheck(): Boolean {
        return entityCore.healthcheck()
    }
}
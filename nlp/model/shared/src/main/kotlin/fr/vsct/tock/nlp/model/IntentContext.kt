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

package fr.vsct.tock.nlp.model

import fr.vsct.tock.nlp.core.Application
import fr.vsct.tock.nlp.core.BuildContext
import fr.vsct.tock.nlp.core.CallContext
import fr.vsct.tock.nlp.core.NlpEngineType
import fr.vsct.tock.nlp.core.quality.TestContext
import fr.vsct.tock.nlp.model.IntentContext.IntentContextKey
import java.util.Locale


data class IntentContext(val application: Application,
                         override val language: Locale,
                         override val engineType: NlpEngineType) : ClassifierContext<IntentContextKey> {

    constructor(callContext: CallContext) : this(callContext.application, callContext.language, callContext.engineType)

    constructor(testContext: TestContext) : this(testContext.callContext)

    constructor(buildContext: BuildContext) : this(buildContext.application, buildContext.language, buildContext.engineType)

    data class IntentContextKey(val applicationName: String,
                                val language: Locale,
                                val nlpEngineType: NlpEngineType) : ClassifierContextKey {
        override fun id(): String {
            return "$applicationName-$language-${nlpEngineType.name}"
        }
    }

    override fun key(): IntentContextKey {
        return IntentContextKey(application.name, language, engineType)
    }
}
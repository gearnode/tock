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

import {Component, Inject} from "@angular/core";
import {Log, LogsQuery, PaginatedResult} from "../model/nlp";
import {ScrollComponent} from "../scroll/scroll.component";
import {StateService} from "../core/state.service";
import {NlpService} from "../nlp-tabs/nlp.service";
import {PaginatedQuery} from "../model/commons";
import {Observable} from "rxjs/Observable";
import {MD_DIALOG_DATA, MdDialog, MdDialogRef} from "@angular/material";
import {ApplicationConfig} from "../core/application.config";
import {Router} from "@angular/router";

@Component({
  selector: 'tock-logs',
  templateUrl: './logs.component.html',
  styleUrls: ['./logs.component.css']
})
export class LogsComponent extends ScrollComponent<Log> {

  title: string = "Logs";
  text: string;

  constructor(state: StateService,
              private nlp: NlpService,
              private dialog: MdDialog,
              private config: ApplicationConfig,
              private router: Router) {
    super(state);
  }

  search(query: PaginatedQuery): Observable<PaginatedResult<Log>> {
    return this.nlp.searchLogs(new LogsQuery(
      query.namespace,
      query.applicationName,
      query.language,
      query.start,
      query.size,
      this.text));
  }


  dataEquals(d1: Log, d2: Log): boolean {
    return d1.request === d2.request;
  }

  displayConversation(log: Log) {
    this.router.navigate(
      [this.config.displayDialogUrl],
      {
        queryParams: {
          dialogId: log.dialogId,
          text: log.error ? log.textRequest() : log.sentence.text
        }
      }
    );
  }

  displayFullLog(log: Log) {
    this.dialog.open(DisplayFullLogComponent, {
      data: {
        request: log.requestDetails(),
        response: log.responseDetails()
      }
    })
  }
}

@Component({
  selector: 'tock-display-full-log',
  template: `<h1 md-dialog-title>Request Full Log</h1>
  <div md-dialog-content>
    Request:
    <pre>{{data.request}}</pre>
    Response:
    <pre>{{data.response}}</pre>
  </div>
  <div md-dialog-actions>
    <button md-raised-button md-dialog-close color="primary">Close</button>
  </div>`
})
export class DisplayFullLogComponent {

  constructor(public dialogRef: MdDialogRef<DisplayFullLogComponent>,
              @Inject(MD_DIALOG_DATA) public data: any) {

  }


}

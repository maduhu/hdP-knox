/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway.hadoopauth;

import org.apache.hadoop.gateway.i18n.messages.Message;
import org.apache.hadoop.gateway.i18n.messages.MessageLevel;
import org.apache.hadoop.gateway.i18n.messages.Messages;

@Messages(logger="org.apache.hadoop.gateway.provider.global.hadoopauth")
public interface HadoopAuthMessages {
  
  @Message( level = MessageLevel.INFO, text = "Initializing Hadoop Auth Property, name: {0},  value: {1}" )
  void initializingHadoopAuthProperty(String name, String value);
  
  @Message( level = MessageLevel.DEBUG, text = "Hadoop Authentication Asserted Principal: {0}" )
  void hadoopAuthAssertedPrincipal(String name);
  
}

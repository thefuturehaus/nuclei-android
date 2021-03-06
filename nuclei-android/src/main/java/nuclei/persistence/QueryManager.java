/**
 * Copyright 2017 YouVersion
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
package nuclei.persistence;

import nuclei.persistence.adapter.PersistenceListAdapter;

public interface QueryManager {

    <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener, QueryArgs args);

    <T> int executeQuery(Query<T> query, PersistenceList.Listener<T> listener);

    <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> listener, QueryArgs args);

    <T> int executeQuery(Query<T> query, PersistenceListAdapter<T> listener);

}

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

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;

@TargetApi(11)
public class SelectQueryBuilder<T> extends QueryBuilder<T> {

    SelectQueryBuilder(Context context, Query<T> query) {
        super(context, query);
    }

    public Cursor execute() {
        validate();
        return query.execute(context.getContentResolver(), args, orderBy);
    }

}

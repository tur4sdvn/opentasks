/*
 * Copyright 2017 dmfs GmbH
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

package org.dmfs.tasks.detailsscreen;

import android.content.ContentProviderClient;
import android.content.Context;
import android.net.Uri;

import org.dmfs.android.bolts.color.Color;
import org.dmfs.android.contentpal.Projection;
import org.dmfs.android.contentpal.RowDataSnapshot;
import org.dmfs.android.contentpal.RowSet;
import org.dmfs.android.contentpal.projections.Composite;
import org.dmfs.android.contentpal.references.RowUriReference;
import org.dmfs.jems.iterable.decorators.Mapped;
import org.dmfs.opentaskspal.readdata.EffectiveDueDate;
import org.dmfs.opentaskspal.readdata.EffectiveTaskColor;
import org.dmfs.opentaskspal.readdata.Id;
import org.dmfs.opentaskspal.readdata.PercentComplete;
import org.dmfs.opentaskspal.readdata.TaskTitle;
import org.dmfs.opentaskspal.rowsets.Subtasks;
import org.dmfs.opentaskspal.views.TasksView;
import org.dmfs.optional.Optional;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.tasks.contract.TaskContract.Tasks;
import org.dmfs.tasks.readdata.CpQuery;
import org.dmfs.tasks.readdata.CpQuerySource;
import org.dmfs.tasks.utils.rxjava.DelegatingSingle;
import org.dmfs.tasks.utils.rxjava.Offloading;

import io.reactivex.Single;

import static org.dmfs.provider.tasks.AuthorityUtil.taskAuthority;


/**
 * {@link Single} that fetches the data needed for showing the subtasks UI section of the details screen.
 *
 * @author Gabor Keszthelyi
 */
public final class SubtasksViewParamsSource extends DelegatingSingle<SubtasksView.Params>
{
    public SubtasksViewParamsSource(Context context, Uri taskUri, Color taskListColor)
    {
        super(new Offloading<>(
                new CpQuerySource<>(context.getApplicationContext(), new SubtasksCpQuery(taskUri, BasicSubtaskViewParams.SUBTASK_PROJECTION))
                        .map(subtaskRows -> new BasicSubtasksViewParams(taskListColor, subtaskRows))));
    }


    private static final class SubtasksCpQuery implements CpQuery<Tasks>
    {
        private final Uri mTaskUri;
        private final Projection<Tasks> mProjection;


        private SubtasksCpQuery(Uri taskUri, Projection<Tasks> projection)
        {
            mTaskUri = taskUri;
            mProjection = projection;
        }


        @Override
        public RowSet<Tasks> rowSet(ContentProviderClient client, Context appContext)
        {
            return new Subtasks(new TasksView(taskAuthority(appContext), client), mProjection, new RowUriReference<>(mTaskUri));
        }
    }


    private static final class BasicSubtasksViewParams implements SubtasksView.Params
    {
        private final Color mTaskListColor;
        private final Iterable<RowDataSnapshot<Tasks>> mSubtaskRows;


        private BasicSubtasksViewParams(Color taskListColor, Iterable<RowDataSnapshot<Tasks>> subtaskRows)
        {
            mTaskListColor = taskListColor;
            mSubtaskRows = subtaskRows;
        }


        @Override
        public Color taskListColor()
        {
            return mTaskListColor;
        }


        @Override
        public Iterable<SubtaskView.Params> subtasks()
        {
            return new Mapped<>(BasicSubtaskViewParams::new, mSubtaskRows);
        }
    }


    private static final class BasicSubtaskViewParams implements SubtaskView.Params
    {

        private static final Projection<Tasks> SUBTASK_PROJECTION = new Composite<>(
                Id.projection(),
                TaskTitle.PROJECTION,
                EffectiveDueDate.PROJECTION,
                EffectiveTaskColor.PROJECTION,
                PercentComplete.PROJECTION
        );

        private final RowDataSnapshot<Tasks> mRowDataSnapshot;


        private BasicSubtaskViewParams(RowDataSnapshot<Tasks> rowDataSnapshot)
        {
            mRowDataSnapshot = rowDataSnapshot;
        }


        @Override
        public Long id()
        {
            return new Id(mRowDataSnapshot).value();
        }


        @Override
        public Optional<CharSequence> title()
        {
            return new TaskTitle(mRowDataSnapshot);
        }


        @Override
        public Optional<DateTime> due()
        {
            return new EffectiveDueDate(mRowDataSnapshot);
        }


        @Override
        public Color color()
        {
            return new EffectiveTaskColor(mRowDataSnapshot);
        }


        @Override
        public Optional<Integer> percentComplete()
        {
            return new PercentComplete(mRowDataSnapshot);
        }
    }

}

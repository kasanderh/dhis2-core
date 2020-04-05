package org.hisp.dhis.tracker.validation.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.PreheatHelper;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventDateValidationHook
    extends AbstractTrackerValidationHook
{
    @Override
    public int getOrder()
    {
        return 302;
    }

    @Override
    public List<TrackerErrorReport> validate( TrackerBundle bundle )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( bundle, this.getClass() );
        User actingUser = bundle.getPreheat().getUser();

        for ( Event event : bundle.getEvents() )
        {
            reporter.increment( event );

            ProgramStageInstance programStageInstance = PreheatHelper
                .getProgramStageInstance( bundle, event.getEvent() );
            Program program = PreheatHelper.getProgram( bundle, event.getProgram() );

            if ( EventStatus.ACTIVE == event.getStatus() && event.getEventDate() == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1031 )
                    .addArg( event ) );
                continue;
            }

            if ( program == null )
            {
                continue;
            }

            validateDateFormat( reporter, event );
            validateExpiryDays( reporter, event, program, programStageInstance, actingUser );
            validatePeriodType( reporter, event, program, programStageInstance );
        }

        return reporter.getReportList();
    }

    private void validateExpiryDays( ValidationErrorReporter errorReporter, Event event, Program program,
        ProgramStageInstance programStageInstance, User actingUser )
    {
        Objects.requireNonNull( actingUser, Constants.USER_CANT_BE_NULL );
        Objects.requireNonNull( event, Constants.EVENT_CANT_BE_NULL );
        Objects.requireNonNull( program, Constants.PROGRAM_CANT_BE_NULL );

        if ( (program.getCompleteEventsExpiryDays() > 0 && EventStatus.COMPLETED == event.getStatus())
            || (programStageInstance != null && EventStatus.COMPLETED == programStageInstance.getStatus()) )
        {
            if ( actingUser.isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
            {
                return;
            }

            Date referenceDate = null;

            if ( programStageInstance != null )
            {
                referenceDate = programStageInstance.getCompletedDate();
            }

            else if ( event.getCompletedDate() != null )
            {
                referenceDate = DateUtils.parseDate( event.getCompletedDate() );
            }

            if ( referenceDate == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1042 )
                    .addArg( event ) );
            }

            if ( (new Date()).after(
                DateUtils.getDateAfterAddition( referenceDate, program.getCompleteEventsExpiryDays() ) ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1043 )
                    .addArg( event ) );
            }
        }
    }

    private void validatePeriodType( ValidationErrorReporter errorReporter, Event event,
        Program program, ProgramStageInstance programStageInstance )
    {
        Objects.requireNonNull( event, Constants.EVENT_CANT_BE_NULL );
        Objects.requireNonNull( program, Constants.PROGRAM_CANT_BE_NULL );

        PeriodType periodType = program.getExpiryPeriodType();

        if ( periodType == null || program.getExpiryDays() == 0)
        {
            return;
        }

        if ( programStageInstance != null )
        {
            Date today = new Date();

            if ( programStageInstance.getExecutionDate() == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1044 )
                    .addArg( event ) );
            }

            Period period = periodType.createPeriod( programStageInstance.getExecutionDate() );

            if ( today.after( DateUtils.getDateAfterAddition( period.getEndDate(), program.getExpiryDays() ) ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1045 )
                    .addArg( program ) );
            }
        }
        else
        {
            String referenceDate = event.getEventDate() != null ? event.getEventDate() : event.getDueDate();
            if ( referenceDate == null )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1046 )
                    .addArg( event ) );
            }

            Period period = periodType.createPeriod( new Date() );

            if ( DateUtils.parseDate( referenceDate ).before( period.getStartDate() ) )
            {
                errorReporter.addError( newReport( TrackerErrorCode.E1047 )
                    .addArg( event ) );
            }
        }
    }

    private void validateDateFormat( ValidationErrorReporter errorReporter, Event event )
    {
        Objects.requireNonNull( event, Constants.EVENT_CANT_BE_NULL );

        if ( event.getDueDate() != null && isNotValidDateString( event.getDueDate() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1051 )
                .addArg( event.getDueDate() ) );
        }

        if ( event.getEventDate() != null && isNotValidDateString( event.getEventDate() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1052 )
                .addArg( event.getEventDate() ) );
        }

        if ( event.getCreatedAtClient() != null && isNotValidDateString( event.getCreatedAtClient() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1053 )
                .addArg( event.getCreatedAtClient() ) );
        }

        if ( event.getLastUpdatedAtClient() != null && isNotValidDateString( event.getLastUpdatedAtClient() ) )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1054 )
                .addArg( event.getLastUpdatedAtClient() ) );
        }
    }
}
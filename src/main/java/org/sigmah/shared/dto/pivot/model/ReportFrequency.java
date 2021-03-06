package org.sigmah.shared.dto.pivot.model;

/*
 * #%L
 * Sigmah
 * %%
 * Copyright (C) 2010 - 2016 URD
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

/**
 * @author Alex Bertram (akbertram@gmail.com)
 */
public enum ReportFrequency {

    /**
     * The report is not bound to a time frame
     *
     */
    NotDateBound,

    /**
     *
     * The time frame of the report will be monthly.
     */
    Monthly,

    /**
     * The time frame of the report is weekly
     */
    Weekly,

    /**
     * The time frame of the report is daily
     */
    Daily,

    /**
     * The time frame of the report is to be defined by an arbitrary
     * date range.
     *
     * (These types of reports cannot be "subscribed" to)
     */
    Adhoc;

}

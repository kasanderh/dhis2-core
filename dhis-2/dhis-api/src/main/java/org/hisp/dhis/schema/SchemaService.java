package org.hisp.dhis.schema;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface SchemaService
{
    /**
     * Get schema which has been generated by a SchemaDescriptor.
     *
     * @param klass Class to get for
     * @return Schema for class, or null
     * @see org.hisp.dhis.schema.SchemaDescriptor
     */
    Schema getSchema( Class<?> klass );

    /**
     * Get schema if it has been described by a SchemaDescriptor, if not, it will
     * generate a Schema dynamically from the class. Only the properties part of the
     * Schema will be usable (together with parts which can be auto-generated like isIdentifiableObject).
     *
     * @param klass Class to get for
     * @return Schema for class, or null
     * @see org.hisp.dhis.schema.SchemaDescriptor
     */
    Schema getDynamicSchema( Class<?> klass );

    /**
     * Get schema which has been generated by a SchemaDescriptor by singular name.
     *
     * @param name Name to get Schema for, will be matched against Schema.getSingular().
     * @return Schema for class, or null
     * @see org.hisp.dhis.schema.SchemaDescriptor
     */
    Schema getSchemaBySingularName( String name );

    /**
     * Get schema which has been generated by a SchemaDescriptor by singular name.
     *
     * @param name Name to get Schema for, will be matched against Schema.getSingular().
     * @return Schema for class, or null
     * @see org.hisp.dhis.schema.SchemaDescriptor
     */
    Schema getSchemaByPluralName( String name );

    /**
     * Get all available schemas (which are generated with a schema descriptor).
     *
     * @return List of all available schemas
     */
    List<Schema> getSchemas();

    /**
     * Get all available schemas (which are generated with a schema descriptor).
     *
     * @return List of all available schemas
     */
    List<Schema> getSortedSchemas();

    /**
     * Get all available schemas which have the metadata property set to true.
     *
     * @return List of all available metadata schemas
     */
    List<Schema> getMetadataSchemas();
}

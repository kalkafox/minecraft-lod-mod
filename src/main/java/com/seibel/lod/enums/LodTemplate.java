/*
 *    This file is part of the LOD Mod, licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.seibel.lod.enums;

import com.seibel.lod.builders.lodTemplates.AbstractLodTemplate;
import com.seibel.lod.builders.lodTemplates.CubicLodTemplate;
import com.seibel.lod.builders.lodTemplates.DynamicLodTemplate;
import com.seibel.lod.builders.lodTemplates.TriangularLodTemplate;

/**
 * Cubic, Triangular, Dynamic
 * 
 * @author James Seibel
 * @version 06-16-2021
 */
public enum LodTemplate
{
	// used for position

	/** Chunks are rendered as
	 * rectangular prisms. */
	CUBIC(new CubicLodTemplate()),
	
	/** Chunks smoothly transition between
	 * each other. */
	TRIANGULAR(new TriangularLodTemplate()),
	
	/** Chunks smoothly transition between
	 * each other, unless a neighboring chunk
	 * is at a significantly different height. */
	DYNAMIC(new DynamicLodTemplate());
	
	
	public final AbstractLodTemplate template;
	
	private LodTemplate(AbstractLodTemplate newTemplate)
	{
		template = newTemplate;
	}
	
	
	public int getBufferMemoryForSingleLod(LodDetail detail)
	{
		return template.getBufferMemoryForSingleLod(detail);
	}
}

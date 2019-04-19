/*******************************************************************************
 * Copyright (c) 2015-2018 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

//
// @author George A. Shulinok <sgazeos@gmail.com>, created on 4/18/2019.
//

#include <op_boilerplate.h>
#if NOT_EXCLUDED(OP_barnes_symmetrized)

#include <ops/declarable/CustomOperations.h>
#include <ops/declarable/helpers/BarnesHutTsne.h>

namespace nd4j {
namespace ops  {
		
		CUSTOM_OP_IMPL(barnes_symmetrized, 3, 1, false, 0, 0) {
    		auto rowP  = INPUT_VARIABLE(0);
            auto colP  = INPUT_VARIABLE(1);
            auto valP  = INPUT_VARIABLE(2);

    		auto output = OUTPUT_VARIABLE(0);

	 	 	helpers::barnes_symmetrize(rowP, colP, valP, output);

		    return Status::OK();
		}

		DECLARE_TYPES(barnes_symmetrized) {
			getOpDescriptor()
				->setAllowedInputTypes(nd4j::DataType::ANY)
				->setSameMode(true);
		}

		DECLARE_SHAPE_FN(barnes_symmetrized) {
    		auto rowPShapeInfo = inputShape->at(0);
            Nd4jLong* outShapeInfo;

    		return SHAPELIST(outShapeInfo);
		}

}
}

#endif
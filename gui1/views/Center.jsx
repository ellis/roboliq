'use babel'

import _ from 'lodash';
import constants from './constants.jsx';
import Home from './Home.jsx';
import ListsList from './ListsList.jsx';
import ProductList from './ProductList.jsx';

const {scoreProductArray, scoreProduct} = require('../../amazon/scoreFuncs.js');

const Center = ({state, controls}) => {
	const ui = state.ui;
	const view1 = ui.view1 || constants.VIEW1_HOME;
	const handlers = {
		[constants.VIEW1_BUCKETS]: () => (<OtherCenter state={state} controls={controls}/>),
		[constants.VIEW1_CATEGORIES]: () => (<CategoriesCenter state={state} controls={controls}/>),
		[constants.VIEW1_HOME]: () => (<HomeCenter state={state} controls={controls}/>),
		[constants.VIEW1_SUBCATEGORIES]: () => (<SubcategoriesCenter listType="category" state={state} controls={controls}/>),
		[constants.VIEW1_PRODUCTS]: () => (<ProductsCenter state={state} controls={controls}/>),
		[constants.VIEW1_SPECIAL]: () => (<SpecialCenter state={state} controls={controls}/>),
		[constants.VIEW1_NICHES]: () => (<NichesCenter state={state} controls={controls}/>)
	};
	const handler = handlers[view1];
	if (!handler) {
		console.log("ERROR:")
		console.log({ui, view1, handlers})
	}
	console.log({ui, view1})
	const result = handler();
	return <div>
		{result}
		{(_.get(state, ["ui", "views", view1, "viewMode"]) === "products")
			? <div>
					<hr/>
					{(function() {
						const products = _.get(state, ["ui", "products"], []);
						const score = scoreProductArray(products);
						// console.log({score})
						if (score.productScores.length > 0) {
							return <div>
								<b>Turnover Score: {JSON.stringify(_.omit(score, "productScores"))}</b>
								{score.productScores.map(data => <div key={data.id}>{JSON.stringify(data)}</div> )}
							</div>;
						}
					})()}
				</div>
			: null
		}
	</div>;
};

const CategoriesCenter = ({state, controls}) => {
	const view1 = _.get(state, ["ui", "view1"]);
	const ui = _.get(state, ["ui", "views", view1], {});
	const category = ui.listId;
	const niches = _.get(state, ["data", "niches"], []);
	if (category) {
		return <div>
			<h1>Category: {category}</h1>

			<ProductList products={_.get(state, ["ui", "products"])} controls={controls} niches={niches}/>
		</div>
	}
	else {
		const categories = _.get(state, ["data", "categories"], []);
		return <div>
			<h1>Categories</h1>

			<ul>
			{
				categories.map(category => {
					return <li key={category}><a onClick={() => controls.onSetUiCategory(category)}>{category}</a></li>
				})
			}
			</ul>
		</div>;
	}
	return <div>
		<h1>Categories</h1>


	</div>
};

const HomeCenter = ({state, controls}) => {
	 return <Home state={state} controls={controls}/>;
};

function makeFilterDescription(filter) {
	function num(label, property) {
		const min = property+"Min";
		const max = property+"Max";
		let s = "";
		if (_.isNumber(filter[min]) || _.isNumber(filter[max])) {
			s = s + "Price ";
			if (_.isNumber(filter[min]) && _.isNumber(filter[max]))
				s = s + `between ${filter[min]} and ${filter[max]}`;
			else if (_.isNumber(filter[min]))
				s = s + `<=${filter[min]}`;
			else if (_.isNumber(filter[max]))
				s = s + `>=${filter[max]}`
		}
		if (s.length > 0)
			s = s + ";"
		return s;
	}

	let s = "";

	s = s + num("Price", "price") + num("Rank", "rank") + num("Size", "size") + num("Weight", "weight");
	return s;
}

const NichesCenter = ({state, controls}) => {
	const view1 = _.get(state, ["ui", "view1"]);
	const ui = _.get(state, ["ui", "views", view1], {});
	// console.log({t: "NichesCenter", ui})
	// const niches = _.get(state, ["data", "niches"], []);
	const niche = ui.listId;
	const list = ui.list;
	const niches = _.get(state, ["data", "niches"], []);
	if (list) {
		return <div>
			<h1>Niche: {list.title}</h1>

			<ListsList
				lists={[list]}
				controls={controls}
			/>

			<hr/>

			<ProductList products={_.get(state, ["ui", "products"])} controls={controls} niches={niches}/>
		</div>;
	}
	else {
		return <div>
			<h1>Niches</h1>

			<ListsList
				lists={ui.lists || []}
				controls={controls}
			/>
		</div>;
	}
};

const OtherCenter = ({state, controls}) => {
	const niches = _.get(state, ["data", "niches"], []);
	return
		<ProductList products={_.get(state, ["ui", "products"])} controls={controls} niches={niches}/>
};

const ProductsCenter = ({state, controls}) => {
	const view1 = _.get(state, ["ui", "view1"]);
	const ui = _.get(state, ["ui", "views", view1], {});
	const filterDescription = makeFilterDescription(ui.filter);
	const niches = _.get(state, ["data", "niches"], []);
	return <div>
		Selected filter: {filterDescription}

		<ProductList products={_.get(state, ["ui", "products"])} controls={controls} niches={niches}/>
	</div>;
}

const SpecialCenter = ({state, controls}) => {
	const niches = _.get(state, ["data", "niches"], []);
	return <div>
		<h1>Private Label List</h1>
		<ProductList products={_.get(state, ["ui", "products"])} controls={controls} niches={niches}/>
	</div>;
};

const SubcategoriesCenter = ({listType, state, controls}) => {
	const view1 = _.get(state, ["ui", "view1"]);
	const ui = _.get(state, ["ui", "views", view1], {});
	console.log({t: "SubcategoriesCenter", ui})
	const list = ui.list;
	const niches = _.get(state, ["data", "niches"], []);
	if (list) {
		const url = `https://www.amazon.${list.site}/gp/bestsellers/${list.category}/${list.node || ""}`;
		return <div>
			<h1>Subcategory: {list.title}</h1>
			<button onClick={controls.onSetUiListPrev}>&lt;&lt;</button>
			<button onClick={controls.onSetUiListNext}>&gt;&gt;</button>
			<span style={{fontSize: "70%", color: "#888"}}><a href="" onClick={(e) => {e.preventDefault(); controls.openBrowser(url)}}>{url}</a></span>

			<ListsList
				lists={[list]}
				controls={controls}
			/>

			<hr/>

			<ProductList products={_.get(state, ["ui", "products"])} controls={controls} niches={niches}/>
		</div>;
	}
	else {
		return <div>
			<h1>Subcategories</h1>

			<ListsList
				lists={ui.lists || []}
				controls={controls}
			/>
		</div>;
	}
};


export default Center;

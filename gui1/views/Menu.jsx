'use babel'

import _ from 'lodash';
import {List} from 'immutable';

import constants from './constants.jsx';

const Menu = React.createClass({
	render: function() {
		const { state, controls, onReload, onViewChange, onSetUiCategory, onNicheChange, onFilterChange, onMoveToNever } = this.props;
		const categories = _.get(state, ["data", "categories"], []);
		const niches = _.get(state, ["data", "niches"], []);
		const view1 = _.get(state, ["ui", "view1"]);
		const ui = _.get(state, ["ui", "views", view1], {});
		const viewMode = ui.viewMode;
		const filter = ui.filter || {};
		console.log({view1, ui, filter, categories, niches})
		// const nicheNames = _.get(state, ["ui", "views", "niches", "lists"], List()).map(item => item.id);
		// console.log({nicheNames})

		return <div>
			<datalist id="categoryNames">
				{categories.map(name => <option key={"categoryName:"+name} value={name}/>)}
			</datalist>
			<datalist id="nicheNames">
				{niches.map(name => <option key={"nicheName:"+name} value={name}/>)}
			</datalist>
			<div style={{display: "inline-block", verticalAlign: "top"}}>
				<button onClick={() => controls.onSetUiView1(constants.VIEW1_PRODUCTS)}>Products</button><br/>
				<button onClick={() => controls.onSetUiView1(constants.VIEW1_CATEGORIES)}>Categories</button><br/>
				<button onClick={() => controls.onSetUiView1(constants.VIEW1_SUBCATEGORIES)}>Subcategories</button><br/>
				<button onClick={() => controls.onSetUiView1(constants.VIEW1_NICHES)}>Niches</button><br/>
				<button onClick={() => controls.onSetUiView1(constants.VIEW1_BUCKETS)}>Buckets</button><br/>
				<button onClick={() => controls.onSetUiView1(constants.VIEW1_SPECIAL)}>Private Label</button>
			</div>{
				(viewMode === "lists")
					? (ui.filterEdit)
						? <ListFilterEditing state={state} editing={true} controls={controls} onFilterChange={onFilterChange}/>
						: <ListFilterDisplay state={state} editing={false} controls={controls} onFilterChange={onFilterChange}/>
				: (viewMode === "products")
					? (ui.filterEdit)
						? <ProductFilterEditing state={state} editing={true} controls={controls} onFilterChange={onFilterChange}/>
						: <ProductFilterDisplay state={state} editing={false} controls={controls} onFilterChange={onFilterChange}/>
				: null
			}

			<div style={{display: "inline-block", float: "right"}}>
				<button onClick={onReload}>Reload</button>
				<button onClick={onMoveToNever}>Move bucketless to Never</button>
				<br/>
				<input className="awesomplete" ref="niche" type="text" placeholder="niche" data-minchars="1" data-list="#nicheNames"/> <button onClick={this.setNiche}>Set Niche on Visible</button>
			</div>
		</div>;
	},
	componentDidUpdate: function() {
		console.log("Menu.componentDidUpdate()")
		const awesomplete = new Awesomplete(this.refs.niche);
		awesomplete.evaluate();
		awesomplete.close();
	},
	onSubmit: function(e) {
		console.log("onSubmit");
		e.preventDefault();
		const num = (x) => _.isEmpty(x.value) ? null : Number(x.value);
		const str = (x) => _.isEmpty(x.value) ? null : x.value;
		const filter = {
			priceMin: num(this.refs.priceMin),
			priceMax: num(this.refs.priceMax),
			rankMin: num(this.refs.rankMin),
			rankMax: num(this.refs.rankMax),
			allowUnranked: this.refs.allowUnranked.checked,
			sizeMin: num(this.refs.sizeMin),
			sizeMax: num(this.refs.sizeMax),
			weightMin: num(this.refs.weightMin),
			weightMax: num(this.refs.weightMax),
			asins: str(this.refs.asins),
			titleWords: str(this.refs.titleWords),
			category: str(this.refs.category),
			buckets: [...this.refs.buckets.options].filter(o => o.selected).map(o => o.value)
		};
		console.log({filter});
		this.props.onFilterChange(filter);
	},
	setNiche: function(e) {
		e.preventDefault();
		const niche = _.isEmpty(this.refs.niche.value) ? null : this.refs.niche.value;
		this.props.onAssignNiche(niche);
	}
});

const makeListFilter = (editing) => React.createClass({
	render: function() {
		const {state, controls, onFilterChange} = this.props;
		const categories = _.get(state, ["data", "categories"], []);
		const niches = _.get(state, ["data", "niches"], []);
		const view1 = _.get(state, ["ui", "view1"]);
		const ui = _.get(state, ["ui", "views", view1], {});
		const filter = ui.listFilter || {};
		console.log({view1, ui, filter});
		const valueName = (editing) ? "defaultValue" : "value";
		console.log({editing, valueName})
		const readOnly = !editing;
		return <div style={{display: "inline-block"}}>
			<table>
				<tbody>
					<tr>
						<td>
							<form id="filterForm" onSubmit={this.onSubmit}>
								<div style={{display: "inline-block", verticalAlign: "top"}}>
									<label>N:</label>
									<input ref="nMin" type="number" placeholder="min" className="size1" {...{[valueName]: filter.nMin || "", readOnly}}/>
									<input ref="nMax" type="number" placeholder="max" className="size1" {...{[valueName]: filter.nMax || "", readOnly}}/> <br/>

									<label>Turnover:</label>
									<input ref="turnoverMin" type="number" placeholder="min" className="size1" {...{[valueName]: filter.turnoverMin || "", readOnly}}/>
									<input ref="turnoverMax" type="number" placeholder="max" className="size1" {...{[valueName]: filter.turnoverMax || "", readOnly}}/> <br/>

									<label>Norm2:</label>
									<input ref="norm2Min" type="number" placeholder="min" className="size1" step="0.1" {...{[valueName]: filter.norm2Min || "", readOnly}}/>
									<input ref="norm2Max" type="number" placeholder="max" className="size1" step="0.1" {...{[valueName]: filter.norm2Max || "", readOnly}}/> <br/>

									<label>Title:</label>
									<input ref="titleWords" type="text" className="size2" {...{[valueName]: filter.titleWords || "", readOnly}}/> <br/>

									{
										(editing)
											? <div>
													<input type="submit" value="Search"/>
													<button onClick={this.onCancel}>Cancel</button>
												</div>
											: <input type="submit" value="Edit"/>
									}
								</div><div style={{display: "inline-block", verticalAlign: "top"}}>
									Buckets:<br/>
									<select ref="buckets" multiple={true} {...{[valueName]: filter.buckets || ["notNever"], readOnly}}>
										<option value="notNever">not-Never</option>
										<option value="everything">Everything</option>
										<option value="undefined">Undefined</option>
										<option value="current">Current</option>
										<option value="soon">Soon</option>
										<option value="later">Later</option>
										<option value="never">Never</option>
									</select>
								</div>
							</form>
						</td>
						<td valign="top" style={{verticalAlign: "text-top"}}>
						</td>
					</tr>
				</tbody>
			</table>
		</div>;
	},
	onSubmit: function(e) {
		console.log("onSubmit");
		e.preventDefault();
		if (editing) {
			const num = (x) => _.isEmpty(x.value) ? null : Number(x.value);
			const str = (x) => _.isEmpty(x.value) ? null : x.value;
			const filter = {
				nMin: num(this.refs.nMin),
				nMax: num(this.refs.nMax),
				turnoverMin: num(this.refs.turnoverMin),
				turnoverMax: num(this.refs.turnoverMax),
				norm2Min: num(this.refs.norm2Min),
				norm2Max: num(this.refs.norm2Max),
				titleWords: str(this.refs.titleWords),
				buckets: [...this.refs.buckets.options].filter(o => o.selected).map(o => o.value)
			};
			console.log({filter});
			this.props.onFilterChange(filter);
			this.props.controls.onSetUiFilterEdit(false);
		}
		else {
			this.props.controls.onSetUiFilterEdit(true);
		}
	},
	onCancel: function(e) {
		console.log("onCancel");
		e.preventDefault();
		this.props.controls.onSetUiFilterEdit(false);
	}
});

const ListFilterDisplay = makeListFilter(false);
const ListFilterEditing = makeListFilter(true);

const makeProductFilter = (editing) => React.createClass({
	render: function() {
		const {state, controls, onFilterChange} = this.props;
		const categories = _.get(state, ["data", "categories"], []);
		const niches = _.get(state, ["data", "niches"], []);
		const view1 = _.get(state, ["ui", "view1"]);
		const ui = _.get(state, ["ui", "views", view1], {});
		const filter = ui.filter || {};
		console.log({view1, ui, filter});
		const valueName = (editing) ? "defaultValue" : "value";
		const checkedName = (editing) ? "defaultChecked": "checked";
		console.log({editing, valueName})
		const readOnly = !editing;
		return <div style={{display: "inline-block"}}>
			<table>
				<tbody>
					<tr>
						<td>
							<form id="filterForm" onSubmit={this.onSubmit}>
								<div style={{display: "inline-block", verticalAlign: "top"}}>
									<label>Price:</label>
									<input ref="priceMin" type="number" placeholder="min" className="size1" {...{[valueName]: filter.priceMin || "", readOnly}}/>
									<input ref="priceMax" type="number" placeholder="max" className="size1" {...{[valueName]: filter.priceMax || "", readOnly}}/> <br/>

									<label>Rank:</label>
									<input ref="rankMin" type="number" placeholder="min" className="size1" {...{[valueName]: filter.rankMin || "", readOnly}}/>
									<input ref="rankMax" type="number" placeholder="max" className="size1" {...{[valueName]: filter.rankMax || "", readOnly}}/>
									<input ref="allowUnranked" type="checkbox" {...{[checkedName]: filter.allowUnranked || false, readOnly}}/> unranked<br/>

									<label>Size:</label>
									<input ref="sizeMin" type="number" placeholder="min" className="size1" {...{[valueName]: filter.sizeMin || "", readOnly}}/>
									<input ref="sizeMax" type="number" placeholder="max" className="size1" {...{[valueName]: filter.sizeMax || "", readOnly}}/> <br/>

									<label>Weight:</label>
									<input ref="weightMin" type="number" placeholder="min" className="size1" step="0.1" {...{[valueName]: filter.weightMin || "", readOnly}}/>
									<input ref="weightMax" type="number" placeholder="max" className="size1" step="0.1" {...{[valueName]: filter.weightMax || "", readOnly}}/> <br/>

									<label>ASINs:</label>
									<input ref="asins" type="text" className="size2" {...{[valueName]: filter.asins || "", readOnly}}/> <br/>

									<label>Title:</label>
									<input ref="titleWords" type="text" className="size2" {...{[valueName]: filter.titleWords || "", readOnly}}/> <br/>

									<label>Category:</label>
									<select ref="category" {...{[valueName]: filter.category || "", readOnly}}>
										<option value=""></option>
										{
											categories.map(category => {
												return <option key={category} value={category}>{category}</option>
											})
										}
									</select>
									<br/>
									{
										(editing)
											? <div>
													<input type="submit" value="Search"/>
													<button onClick={this.onCancel}>Cancel</button>
												</div>
											: <input type="submit" value="Edit"/>
									}
								</div><div style={{display: "inline-block", verticalAlign: "top"}}>
									Buckets:<br/>
									<select ref="buckets" multiple={true} {...{[valueName]: filter.buckets || ["notNever"], readOnly}}>
										<option value="notNever">not-Never</option>
										<option value="everything">Everything</option>
										<option value="undefined">Undefined</option>
										<option value="current">Current</option>
										<option value="soon">Soon</option>
										<option value="later">Later</option>
										<option value="never">Never</option>
									</select>
								</div>
							</form>
						</td>
						<td valign="top" style={{verticalAlign: "text-top"}}>
						</td>
					</tr>
				</tbody>
			</table>
		</div>;
	},
	onSubmit: function(e) {
		console.log("onSubmit");
		e.preventDefault();
		if (editing) {
			const num = (x) => _.isEmpty(x.value) ? null : Number(x.value);
			const str = (x) => _.isEmpty(x.value) ? null : x.value;
			const filter = {
				priceMin: num(this.refs.priceMin),
				priceMax: num(this.refs.priceMax),
				rankMin: num(this.refs.rankMin),
				rankMax: num(this.refs.rankMax),
				allowUnranked: this.refs.allowUnranked.checked,
				sizeMin: num(this.refs.sizeMin),
				sizeMax: num(this.refs.sizeMax),
				weightMin: num(this.refs.weightMin),
				weightMax: num(this.refs.weightMax),
				asins: str(this.refs.asins),
				titleWords: str(this.refs.titleWords),
				category: str(this.refs.category),
				buckets: [...this.refs.buckets.options].filter(o => o.selected).map(o => o.value)
			};
			console.log({filter});
			this.props.onFilterChange(filter);
			this.props.controls.onSetUiFilterEdit(false);
		}
		else {
			this.props.controls.onSetUiFilterEdit(true);
		}
	},
	onCancel: function(e) {
		console.log("onCancel");
		e.preventDefault();
		this.props.controls.onSetUiFilterEdit(false);
	}
});

const ProductFilterDisplay = makeProductFilter(false);
const ProductFilterEditing = makeProductFilter(true);

export default Menu;

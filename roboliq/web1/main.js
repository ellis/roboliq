const tree1 = {
	"strain*": [
		"strain1",
		"strain2"
	]
};

const tree2 = {
	"strain*": [
		"strain1",
		"strain2"
	],
	"media*": [
		"media1",
		"media2"
	]
};

const tree3 = {
	"strain*": [
		"strain1",
		"strain2"
	],
	"media*": {
		"media1": {
			"replicate*": [1,2]
		},
		"media2": {
			"replicate*": [1]
		}
	}
};

const tree4 = {
	conditions: {
		"strain*": [
			"strain1",
			"strain2"
		],
		"media*": {
			"media1": {
				"replicate*": [1,2]
			},
			"media2": {
				"replicate*": [1]
			}
		}
	},
	processing: [
		{}
	]
	replicates: {
		replicate: {
			count: 2
		}
	},
	assign: {
		plate: {
			values: ["plate1", "plate2"],
			random: true,
			groupBy: "strain"
		}
		well: {
			values: ["A01", "B01", "C01", "D01", "A02", "B02", "C02", "D02"],
			random: true
		},
		order: {
			index: true
			random: true
		}
	}
};

function flatten(input, depth = -1) {
	//assert(_.isArray(input));
	let flatter = (_.isArray(input)) ? input : [input];
	let again = true;
	while (again && depth != 0) {
		//console.log({depth})
		again = false;
		flatter = _.flatMap(flatter, (row) => {
			console.log({row})
			//assert(_.isPlainObject(row));
			let rows = [{}];
			_.forEach(row, (value, key) => {
				console.log({key, value})
				if (depth != 0 && _.endsWith(key, "*")) {
					again = true;
					const key2 = key.substring(0, key.length - 1);
					// For each entry in value, make a copy of every row in rows with the properties of the entry
					rows = _.flatMap(rows, x => {
						return _.map(value, (value3, key3) => {
							console.log({key3, value3})
							if (_.isPlainObject(value3)) {
								const value2 = (_.isNumber(key3)) ? key3 + 1 : key3;
								return _.merge({}, x, _.fromPairs([[key2, value2]]), value3);
							}
							else {
								return _.merge({}, x, _.fromPairs([[key2, value3]]));
							}
						});
					});
				}
				else {
					_.forEach(rows, row => { row[key] = value; });
					/*if (key === "reseal") {
						console.log("reseal: "+value)
						console.log(rows)
					}*/
				}
			});
			//console.log({rows})
			return rows;
		});
		if (depth > 0)
			depth--;
	}

	return flatter;
}

const counter = (state = 0, action) => {
	switch(action.type) {
		case "INCREMENT":
			return state + 1;
		case "DECREMENT":
			return state - 1;
		default:
			return state;
	}
}

const { createStore } = Redux;
const store = createStore(counter);

const Table = ({
	table
}) => {
	// Get column names
	const columnMap = {};
	_.forEach(table, row => _.forEach(_.keys(row), key => { columnMap[key] = true; } ));
	const columns = _.keys(columnMap);

	return <table>
		<thead>
			<tr>{_.map(columns, s => <th key={"column_"+s}>{s}</th>)}</tr>
		</thead>
		<tbody>
			{_.map(table, (row, index) => <tr key={"row"+index}>
				{_.map(columns, key => <td key={"row"+index+"_"+key}>{row[key]}</td>)}
			</tr>)}
		</tbody>
	</table>;
};

const Counter = ({
	value,
	onIncrement,
	onDecrement
}) => {
	const result = {hi: {there: value}};
	const tree
		= (value === 0) ? tree1
		: (value === 1) ? tree2
		: tree3;
	const table = flatten(tree);
	return <div>
		<pre>{YAML.stringify(tree, 5, 1)}</pre>
		<Table table={table}/>
		<span>{value}</span>
		<button onClick={onIncrement}>+</button>
		<button onClick={onDecrement}>-</button>
	</div>;
};

const render = () => {
	ReactDOM.render(
		<Counter
			value={store.getState()}
			onIncrement={() => store.dispatch({type: "INCREMENT"})}
			onDecrement={() => store.dispatch({type: "DECREMENT"})}
		/>,
		document.getElementById("root")
	);
};

store.subscribe(render);
render();

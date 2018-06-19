'use babel'

import _ from 'lodash';

const ListsListItem = ({item, controls}) => {
	// console.log({item})
	return <tr>
		<td style={{textAlign: "right"}}>{_.size(item.products)}</td>
		<td style={{textAlign: "right"}}>{(item.turnover || 0).toFixed(0)}</td>
		<td style={{textAlign: "right"}}>{(item.turnoverGross || 0).toFixed(0)}</td>
		{/*<td style={{textAlign: "right"}}>{(item.turnoverNorm1 || 0).toFixed(2)}</td>*/}
		<td style={{textAlign: "right"}}>{(item.turnoverNorm2 || 0).toFixed(2)}</td>
		<td>
			<span
				style={(item.bucket === "current") ? {backgroundColor: "#40ff40"} : null}
				onClick={() => controls.onSetListBucket([item.id], "current")}
			>C</span>/<span
				style={(item.bucket === "soon") ? {backgroundColor: "#80ffff"} : null}
				onClick={() => controls.onSetListBucket([item.id], "soon")}
			>S</span>/<span
				style={(item.bucket === "later") ? {backgroundColor: "#8080ff"} : null}
				onClick={() => controls.onSetListBucket([item.id], "later")}
			>L</span>/<span
				style={(item.bucket === "never") ? {backgroundColor: "#ff8080"} : null}
				onClick={() => controls.onSetListBucket([item.id], "never")}
			>N</span>
		</td>
		<td><span onClick={(event) => {controls.onSetUiList(item)}}>{item.title}</span></td>
	</tr>
};

const ListsList = ({lists, controls}) => {
	// console.log({lists})
	return <table>
		<thead>
			<tr>
				<th><span onClick={() => controls.onSortLists("n")}>N</span></th>
				<th><span onClick={() => controls.onSortLists("turnover")}>Turnover</span></th>
				<th><span onClick={() => controls.onSortLists("turnoverGross")}>Gross</span></th>
				{/*<th><span onClick={() => controls.onSortLists("turnoverNorm1")}>Score1</span></th>*/}
				<th><span onClick={() => controls.onSortLists("turnoverNorm2")}>Score</span></th>
				<th>Bucket</th>
				<th><span onClick={() => controls.onSortLists("title")}>Title</span></th>
			</tr>
		</thead>
		<tbody>
			{lists.map(item => <ListsListItem key={item.id} item={item} controls={controls}/>)}
		</tbody>
	</table>
};

export default ListsList;

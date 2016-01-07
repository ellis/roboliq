var _ = require('lodash');
var should = require('should');
var roboliq = require('../src/roboliq.js')

describe('pipetter', function() {
	describe("pipetter.cleanTips", function() {
		it("should clean tip 1", function() {
			const protocol = {
				roboliq: "v1",
				steps: {
					"1": {
						command: "pipetter.cleanTips",
						items: [
							{syringe: "ourlab.mario.liha.syringe.1", intensity: "light"}
						]
					}
				}
			};
			const result = roboliq.run(["-o", ""], protocol);
			//console.log(JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"1": {
							"command": "pipetter._washTips",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.liha",
							"program": "ourlab.mario.washProgram.light_1000",
							"intensity": "light",
							"syringes": [
								"ourlab.mario.liha.syringe.1",
								"ourlab.mario.liha.syringe.2",
								"ourlab.mario.liha.syringe.3",
								"ourlab.mario.liha.syringe.4"
							]
						},
						"command": "pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"intensity": "light"
							}
						]
					},
					"command": "pipetter.cleanTips",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"intensity": "light"
						}
					]
				}
			});
		});


		it("should clean tips 1 thru 4", () => {
			const protocol = {
				"roboliq": "v1",
				"steps": {
					"1": {
						"command": "pipetter.cleanTips",
						//"equipment": "ourlab.mario.liha",
						"items": [
							{"syringe": "ourlab.mario.liha.syringe.1", "intensity": "flush"},
							{"syringe": "ourlab.mario.liha.syringe.2", "intensity": "flush"},
							{"syringe": "ourlab.mario.liha.syringe.3", "intensity": "flush"},
							{"syringe": "ourlab.mario.liha.syringe.4", "intensity": "flush"}
						]
					},
					"2": {
						"command": "pipetter.cleanTips",
						//"equipment": "ourlab.mario.liha",
						"syringes": ["ourlab.mario.liha.syringe.1","ourlab.mario.liha.syringe.2","ourlab.mario.liha.syringe.3","ourlab.mario.liha.syringe.4"],
						"intensity": "light"
					}
				}
			};
			const result = roboliq.run(["-o", ""], protocol);
			//console.log(JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"1": {
							"command": "pipetter._washTips",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.liha",
							"program": "ourlab.mario.washProgram.flush_1000",
							"intensity": "flush",
							"syringes": [
								"ourlab.mario.liha.syringe.1",
								"ourlab.mario.liha.syringe.2",
								"ourlab.mario.liha.syringe.3",
								"ourlab.mario.liha.syringe.4"
							]
						},
						"command": "pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"intensity": "flush"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"intensity": "flush"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.3",
								"intensity": "flush"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.4",
								"intensity": "flush"
							}
						]
					},
					"command": "pipetter.cleanTips",
					//"equipment": "ourlab.mario.liha",
					"items": [
						{
							"syringe": "ourlab.mario.liha.syringe.1",
							"intensity": "flush"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.2",
							"intensity": "flush"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.3",
							"intensity": "flush"
						},
						{
							"syringe": "ourlab.mario.liha.syringe.4",
							"intensity": "flush"
						}
					]
				},
				"2": {
					"1": {
						"1": {
							"command": "pipetter._washTips",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.liha",
							"program": "ourlab.mario.washProgram.light_1000",
							"intensity": "light",
							"syringes": [
								"ourlab.mario.liha.syringe.1",
								"ourlab.mario.liha.syringe.2",
								"ourlab.mario.liha.syringe.3",
								"ourlab.mario.liha.syringe.4"
							]
						},
						"command": "pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"intensity": "light"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"intensity": "light"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.3",
								"intensity": "light"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.4",
								"intensity": "light"
							}
						]
					},
					"command": "pipetter.cleanTips",
					//"equipment": "ourlab.mario.liha",
					"syringes": [
						"ourlab.mario.liha.syringe.1",
						"ourlab.mario.liha.syringe.2",
						"ourlab.mario.liha.syringe.3",
						"ourlab.mario.liha.syringe.4"
					],
					"intensity": "light"
				}
			});
		});


		it("should clean all tips", () => {
			const protocol = {
				"roboliq": "v1",
				"steps": {
					"1": {
						"command": "pipetter.cleanTips",
						"equipment": "ourlab.mario.liha",
						"intensity": "thorough"
					}
				}
			};
			const result = roboliq.run(["-o", ""], protocol);
			//console.log(JSON.stringify(result.output.steps, null, '\t'));
			should.deepEqual(result.output.steps, {
				"1": {
					"1": {
						"1": {
							"command": "pipetter._washTips",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.liha",
							"program": "ourlab.mario.washProgram.thorough_1000",
							"intensity": "thorough",
							"syringes": [
								"ourlab.mario.liha.syringe.1",
								"ourlab.mario.liha.syringe.2",
								"ourlab.mario.liha.syringe.3",
								"ourlab.mario.liha.syringe.4"
							]
						},
						"2": {
							"command": "pipetter._washTips",
							"agent": "ourlab.mario.evoware",
							"equipment": "ourlab.mario.liha",
							"program": "ourlab.mario.washProgram.thorough_0050",
							"intensity": "thorough",
							"syringes": [
								"ourlab.mario.liha.syringe.5",
								"ourlab.mario.liha.syringe.6",
								"ourlab.mario.liha.syringe.7",
								"ourlab.mario.liha.syringe.8"
							]
						},
						"command": "pipetter.cleanTips|ourlab.mario.evoware|ourlab.mario.liha",
						"agent": "ourlab.mario.evoware",
						"equipment": "ourlab.mario.liha",
						"items": [
							{
								"syringe": "ourlab.mario.liha.syringe.1",
								"intensity": "thorough"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.2",
								"intensity": "thorough"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.3",
								"intensity": "thorough"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.4",
								"intensity": "thorough"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.5",
								"intensity": "thorough"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.6",
								"intensity": "thorough"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.7",
								"intensity": "thorough"
							},
							{
								"syringe": "ourlab.mario.liha.syringe.8",
								"intensity": "thorough"
							}
						]
					},
					"command": "pipetter.cleanTips",
					"equipment": "ourlab.mario.liha",
					"intensity": "thorough"
				}
			});
		});


		it("should set the syringe state to cleaned", () => {
			var protocol = {
				roboliq: "v1",
				objects: {
					ourlab: {
						mario: {
							liha: {
								syringe: {
									1: {
										contaminants: ["A"],
										contents: ["stuff"],
										cleaned: "thorough"
									}
								}
							}
						}
					}
				},
				steps: {
					"1": {
						"command": "pipetter.cleanTips",
						"syringes": ["ourlab.mario.liha.syringe.1"],
						"intensity": "light"
					}
				}
			};
			var result = roboliq.run(["-o", "", "-T"], protocol);
			//console.log(JSON.stringify(result.output, null, '\t'))
			should.deepEqual(result.output.errors, {});
			should.deepEqual(result.output.warnings, {});
			should.deepEqual(result.output.effects, {
				"1.1.1": {
					"ourlab.mario.liha.syringe.1.contaminants": null,
					"ourlab.mario.liha.syringe.1.cleaned": "light",
					"ourlab.mario.liha.syringe.1.contents": null,
					"ourlab.mario.liha.syringe.2.cleaned": "light",
					"ourlab.mario.liha.syringe.3.cleaned": "light",
					"ourlab.mario.liha.syringe.4.cleaned": "light",
				}
			});
		});
	});
});

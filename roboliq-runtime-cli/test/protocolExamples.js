const protocol1 = {
	roboliq: "v1",
	steps: {
		1: {
			command: "system.echo",
			value: "hello",
			1: {
				command: "system._echo",
				value: "hello"
			}
		},
		2: {
			command: "system.echo",
			value: "world",
			1: {
				command: "system._echo",
				value: "world"
			}
		}
	}
};

module.exports = protocol1;

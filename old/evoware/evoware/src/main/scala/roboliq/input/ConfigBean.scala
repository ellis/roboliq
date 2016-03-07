package roboliq.input

import scala.beans.BeanProperty

class ConfigBean {
	@BeanProperty var evowareAgents: java.util.HashMap[String, EvowareAgentBean] = null
	@BeanProperty var aliases: java.util.HashMap[String, String] = null
	@BeanProperty var logic: java.util.ArrayList[String] = null
	@BeanProperty var specs: java.util.HashMap[String, String] = null
	@BeanProperty var deviceToSpec: java.util.ArrayList[java.util.ArrayList[String]] = null
	@BeanProperty var deviceToModelToSpec: java.util.ArrayList[java.util.ArrayList[String]] = null
	@BeanProperty var externalThermocyclers: java.util.ArrayList[String] = null
	@BeanProperty var commandFiles: java.util.ArrayList[String] = null
}

class EvowareAgentBean {
	/** Database ID */
	@BeanProperty var key: String = null
	/** Protocol ID */
	@BeanProperty var name: String = null
	/** Jshop ID */
	@BeanProperty var ident: String = null
	/** Evoware data directory */
	@BeanProperty var evowareDir: String = null
	/** Labware that this robot can use */
	@BeanProperty var labwareModels: java.util.ArrayList[LabwareModelBean] = null
	/** Tip models that this robot can use */
	@BeanProperty var tipModels: java.util.HashMap[String, TipModelBean] = null
	/** This robot's tips */
	@BeanProperty var tips: java.util.ArrayList[TipBean] = null
	/** Table setups for this robot */
	@BeanProperty var tableSetups: java.util.HashMap[String, TableSetupBean] = null
	/** Sealer programs */
	@BeanProperty var transporterBlacklist: java.util.ArrayList[TransporterBlacklistBean] = null
	/** Sealer programs */
	@BeanProperty var sealerProgram: java.util.ArrayList[SealerProgramBean] = null
}

class LabwareModelBean {
	@BeanProperty var name: String = null
	@BeanProperty var label: String = null
	@BeanProperty var evowareName: String = null
}

class TipModelBean {
	//@BeanProperty var name: String = null
	@BeanProperty var min: java.lang.Double = null
	@BeanProperty var max: java.lang.Double = null
}

class TipBean {
	@BeanProperty var row: Integer = null
	@BeanProperty var permanentModel: String = null
	@BeanProperty var models: java.util.ArrayList[String] = null
}

class TableSetupBean {
	/** Path to evoware table file */
	@BeanProperty var tableFile: String = null
	/** Site definitions */
	@BeanProperty var sites: java.util.HashMap[String, SiteBean] = null
	/** List of sites the pipetter can access */
	@BeanProperty var pipetterSites: java.util.ArrayList[String] = null
	/** List of sites the user can directly access */
	@BeanProperty var userSites: java.util.ArrayList[String] = null
}

class SiteBean {
	@BeanProperty var carrier: String = null
	@BeanProperty var grid: Integer = null
	@BeanProperty var site: Integer = null
}

class TransporterBlacklistBean {
	@BeanProperty var roma: Integer = null
	@BeanProperty var vector: String = null
	@BeanProperty var site: String = null
}

class SealerProgramBean {
	//@BeanProperty var name: String = null
	//@BeanProperty var device: String = null
	@BeanProperty var model: String = null
	@BeanProperty var filename: String = null
}
package playground.mmoyo.analysis.counts.chen;

import playground.yu.run.TrCtl;

/**uses Yu transit controler to have counts results**/
public class Counter {

	public static void main(String[] args) {
		String configFile;
		
		//no fragmented
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/no_fragmented/config/config_routedPlans_MoyoParameterized.xml";
			//TrCtl.main(new String[]{configFile});
		
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/no_fragmented/config/config_routedPlans_MoyoTime.xml";
			//TrCtl.main(new String[]{configFile});
		
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/no_fragmented/config/config_routedPlans.xml";
			//TrCtl.main(new String[]{configFile});
		
		//fragmented
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans_MoyoParameterized.xml";
			//TrCtl.main(new String[]{configFile});
		
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans_MoyoTime.xml";
			//TrCtl.main(new String[]{configFile});
			
			//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_1x_subset_xy2links_ptplansonly/fragmented/config/config_routedPlans.xml";
			//TrCtl.main(new String[]{configFile});	
		
		//5x
		//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_5x_subset_xy2links_ptplansonly/config/config_5x_routed_param.xml";
		//TrCtl.main(new String[]{configFile});
		
		configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_5x_subset_xy2links_ptplansonly/config/config_5x_routed_time.xml";
		TrCtl.main(new String[]{configFile});
		
		//configFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/comparison/BerlinBrandenburg/routed_5x_subset_xy2links_ptplansonly/config/config_5x_routed.xml";
		//TrCtl.main(new String[]{configFile});
	
	}
}

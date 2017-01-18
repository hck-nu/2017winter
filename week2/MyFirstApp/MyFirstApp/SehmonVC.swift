//
//  SehmonVC.swift
//  MyFirstApp
//
//  Created by Meg Grasse on 1/11/17.
//  Copyright © 2017 Meg Grasse. All rights reserved.
//

import UIKit

class SehmonVC: UIViewController {

    @IBOutlet weak var sehmonImage: UIImageView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        sehmonImage.image = #imageLiteral(resourceName: "sehmon")

        // Do any additional setup after loading the view.
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

}

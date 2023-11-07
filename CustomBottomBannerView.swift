import UIKit
import MapboxNavigation

protocol CustomBottomBannerViewDelegate: AnyObject {
    func customBottomBannerDidCancel(_ banner: CustomBottomBannerView)
}

class CustomBottomBannerView: UIView {
    let etaLabel: UILabel = {
        let label = UILabel();
        label.font = UIFont.systemFont(ofSize: 45, weight: .medium);
        label.textColor = UIColor.blue
        label.textAlignment = .center
        label.text = "wait"
        return label;
    }()
    
    var eta: String? {
        get {
            return etaLabel.text
        }
        set {
            etaLabel.text = newValue
        }
    }
    
    weak var delegate: CustomBottomBannerViewDelegate?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        onViewLoad()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        onViewLoad()
    }
    
    
    func onViewLoad() -> Void {
        print("OnBottomLoad:: ")
        addSubview(etaLabel);
        etaLabel.translatesAutoresizingMaskIntoConstraints = false
        etaLabel.centerYAnchor.constraint(equalTo: self.centerYAnchor).isActive = true
        etaLabel.centerXAnchor.constraint(equalTo: self.centerXAnchor).isActive = true
//        etaLabel.topAnchor.constraint(equalTo: self.topAnchor, constant: -150).isActive = true
        
        backgroundColor = UIColor.blue
    }
}

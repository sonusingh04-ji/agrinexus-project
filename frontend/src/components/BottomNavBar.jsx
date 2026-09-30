import {
  useLocation,
  useNavigate,
} from "react-router-dom";

import {
  useLanguage,
} from "../context/LanguageContext";

import "./BottomNavBar.css";


function BottomNavBar() {

  const navigate =
      useNavigate();

  const location =
      useLocation();


  const {
    t,
  } = useLanguage();


  const navItems = [

    {
      path: "/home",
      icon: "🏠",
      labelKey: "navigation.home",
    },

    {
      path: "/history",
      icon: "📜",
      labelKey: "navigation.history",
    },

    {
      path: "/copilot",
      icon: "🎤",
      labelKey: "navigation.ask",
    },

    {
      path: "/profile",
      icon: "👤",
      labelKey: "navigation.profile",
    },

  ];


  return (

      <nav
          className="bottom-nav"
          aria-label="Main navigation"
      >

        {navItems.map((item) => {

          const isActive =
              location.pathname ===
              item.path;


          return (

              <button
                  key={item.path}
                  type="button"
                  className={`
                            nav-item
                            ${
                      isActive
                          ? "nav-active"
                          : ""
                  }
                        `}
                  onClick={() =>
                      navigate(
                          item.path
                      )
                  }
                  aria-current={
                    isActive
                        ? "page"
                        : undefined
                  }
              >

                        <span
                            className="nav-icon"
                        >
                            {item.icon}
                        </span>


                <span
                    className="nav-label"
                >
                            {t(
                                item.labelKey
                            )}
                        </span>

              </button>

          );
        })}

      </nav>
  );
}


export default BottomNavBar;
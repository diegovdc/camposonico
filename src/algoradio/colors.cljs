(ns algoradio.colors
  (:require [clojure.string :as str]))

(defn css-color [color]
  (let [colors (-> color
                   (str/replace #"#" "")
                   (str/split #"")
                   rest)
        red (take 2 colors)
        green (take 2 (drop 2 colors))
        blue (take 2 (drop 4 colors))]
    (mapv #(-> (conj % "0x") str/join (js/parseInt 16))
          [red green blue])))


(def hex-colors ["#EFDECD", "#CD9575", "#FDD9B5", "#78DBE2", "#87A96B", "#FFA474", "#FAE7B5", "#9F8170", "#FD7C6E", "#000000", "#ACE5EE", "#1F75FE", "#A2A2D0", "#6699CC", "#0D98BA", "#7366BD", "#DE5D83", "#CB4154", "#B4674D", "#FF7F49", "#EA7E5D", "#B0B7C6", "#FFFF99", "#1CD3A2", "#FFAACC", "#DD4492", "#1DACD6", "#BC5D58", "#DD9475", "#9ACEEB", "#FFBCD9", "#FDDB6D", "#2B6CC4", "#EFCDB8", "#6E5160", "#CEFF1D", "#71BC78", "#6DAE81", "#C364C5", "#CC6666", "#E7C697", "#FCD975", "#A8E4A0", "#95918C", "#1CAC78", "#1164B4", "#F0E891", "#FF1DCE", "#B2EC5D", "#5D76CB", "#CA3767", "#3BB08F", "#FEFE22", "#FCB4D5", "#FFF44F", "#FFBD88", "#F664AF", "#AAF0D1", "#CD4A4C", "#EDD19C", "#979AAA", "#FF8243", "#C8385A", "#EF98AA", "#FDBCB4", "#1A4876", "#30BA8F", "#C54B8C", "#1974D2", "#FFA343", "#BAB86C", "#FF7538", "#FF2B2B", "#F8D568", "#E6A8D7", "#414A4C", "#FF6E4A", "#1CA9C9", "#FFCFAB", "#C5D0E6", "#FDDDE6", "#158078", "#FC74FD", "#F78FA7", "#8E4585", "#7442C8", "#9D81BA", "#FE4EDA", "#FF496C", "#D68A59", "#714B23", "#FF48D0", "#E3256B", "#EE204D", "#FF5349", "#C0448F", "#1FCECB", "#7851A9", "#FF9BAA", "#FC2847", "#76FF7A", "#9FE2BF", "#A5694F", "#8A795D", "#45CEA2", "#FB7EFD", "#CDC5C2", "#80DAEB", "#ECEABE", "#FFCF48", "#FD5E53", "#FAA76C", "#18A7B5", "#EBC7DF", "#FC89AC", "#DBD7D2", "#17806D", "#DEAA88", "#77DDE7", "#FFFF66", "#926EAE", "#324AB2", "#F75394", "#FFA089", "#8F509D", "#FFFFFF", "#A2ADD0", "#FF43A4", "#FC6C85", "#CDA4DE", "#FCE883", "#C5E384", "#FFAE42"])

(def colors (mapv css-color hex-colors))

(defonce color-offset (rand-int 100))

(defn get-color* [sound-id]
  (nth colors (mod (+ color-offset sound-id)
                   (dec (count colors))) '(0 0 0)))

(defn get-color
  #_([sound-id] (get-color sound-id 1))
  ([sound-id opacity]
   (let [rgba (-> (get-color* sound-id)
                  (conj opacity))]
     (str "rgba("(str/join "," rgba) ")"))))

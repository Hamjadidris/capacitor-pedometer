import { PedometerPlugin } from 'capacitor-pedometer';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    PedometerPlugin.echo({ value: inputValue })
}

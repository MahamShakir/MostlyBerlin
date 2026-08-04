import { Component } from "react";


export class ErrorBoundary extends Component {


    constructor(props) {

        super(props);

        this.state = {
            hasError: false,
            error: null
        };
    }


    static getDerivedStateFromError(error) {

        return {
            hasError: true,
            error
        };
    }


    componentDidCatch(error, info) {

        console.error(
            "ErrorBoundary caught:",
            error,
            info
        );
    }


    render() {

        if (this.state.hasError) {

            return (
                this.props.fallback ??
                <div role="alert" style={{ padding: 24 }}>

                    <h2>
                        Something broke on this page.
                    </h2>

                    <p>
                        Please refresh or return to dashboard.
                    </p>

                </div>
            );
        }


        return this.props.children;
    }
}